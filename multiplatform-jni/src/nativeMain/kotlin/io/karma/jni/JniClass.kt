/*
 * Copyright 2024 Karma Krafts & associates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalForeignApi::class)

package io.karma.jni

import co.touchlab.stately.collections.ConcurrentMutableMap
import io.karma.jni.JvmObject.Companion.cast
import jni.JNIEnvVar
import jni.jclass
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret

class JvmClass internal constructor(
    override val handle: jclass?
) : JvmObject {
    private val fields: ConcurrentMutableMap<FieldDescriptor, JvmField> = ConcurrentMutableMap()
    private val methods: ConcurrentMutableMap<MethodDescriptor, JvmMethod> = ConcurrentMutableMap()

    companion object {
        val NULL: JvmClass = JvmClass(null)
        private val cache: ConcurrentMutableMap<jclass, JvmClass> = ConcurrentMutableMap()

        fun fromHandle(handle: jclass?): JvmClass {
            return if (handle == null) NULL
            else cache.getOrPut(handle) { JvmClass(handle) }
        }

        fun fromUnchecked(obj: JvmObject): JvmClass = fromHandle(obj.handle?.reinterpret())

        fun findOrNull(env: JNIEnvVar, type: Type): JvmClass? {
            val handle = memScoped {
                env.pointed?.FindClass?.invoke(env.ptr, allocCString(type.jvmName))
            } ?: return null
            return if (handle in cache) cache[handle]!!
            else JvmClass(handle).apply {
                cache[handle] = this
            }
        }

        fun find(env: JNIEnvVar, type: Type): JvmClass =
            requireNotNull(findOrNull(env, type)) { "Could not find class" }
    }

    fun findFieldOrNull(env: JNIEnvVar, descriptor: FieldDescriptor): JvmField? {
        if (descriptor in fields) return fields[descriptor]
        val handle = memScoped {
            if (descriptor.isStatic) env.pointed?.GetStaticFieldID?.invoke(
                env.ptr,
                handle,
                allocCString(descriptor.name),
                allocCString(descriptor.jvmDescriptor)
            )
            else env.pointed?.GetFieldID?.invoke(
                env.ptr,
                handle,
                allocCString(descriptor.name),
                allocCString(descriptor.jvmDescriptor)
            )
        } ?: return null
        return JvmField(this, descriptor, handle).apply {
            fields[descriptor] = this
        }
    }

    fun findFieldOrNull(env: JNIEnvVar, closure: FieldDescriptorBuilder.() -> Unit): JvmField? =
        findFieldOrNull(env, FieldDescriptor.create(closure))

    fun findField(env: JNIEnvVar, descriptor: FieldDescriptor): JvmField =
        requireNotNull(findFieldOrNull(env, descriptor)) { "Could not find field" }

    fun findField(env: JNIEnvVar, closure: FieldDescriptorBuilder.() -> Unit): JvmField =
        findField(env, FieldDescriptor.create(closure))

    fun findMethodOrNull(env: JNIEnvVar, descriptor: MethodDescriptor): JvmMethod? {
        if (descriptor in methods) return methods[descriptor]
        val handle = memScoped {
            if (descriptor.access == MethodAccess.STATIC) env.pointed?.GetStaticMethodID?.invoke(
                env.ptr,
                handle,
                allocCString(descriptor.name),
                allocCString(descriptor.jvmDescriptor)
            )
            else env.pointed?.GetMethodID?.invoke(
                env.ptr,
                handle,
                allocCString(descriptor.name),
                allocCString(descriptor.jvmDescriptor)
            )
        } ?: return null
        return JvmMethod(this, descriptor, handle).apply {
            methods[descriptor] = this
        }
    }

    fun findMethodOrNull(env: JNIEnvVar, closure: MethodDescriptorBuilder.() -> Unit): JvmMethod? =
        findMethodOrNull(env, MethodDescriptor.create(closure))

    fun findMethod(env: JNIEnvVar, descriptor: MethodDescriptor): JvmMethod =
        requireNotNull(findMethodOrNull(env, descriptor)) { "Could not find method" }

    fun findMethod(env: JNIEnvVar, closure: MethodDescriptorBuilder.() -> Unit): JvmMethod =
        findMethod(env, MethodDescriptor.create(closure))

    fun getType(env: JNIEnvVar): Type { // @formatter:off
        return findMethod(env) {
            name = "getName"
            returnType = Type.get("java.lang.String")
        }.callObject(env, this)
            .cast<JvmString>(env)
            .get(env)
            ?.let(Type::get)
            ?: NullType
    } // @formatter:on

    fun hasAnnotation(env: JNIEnvVar, type: Type): Boolean {
        require(type is ClassType) { "Annotation must be class type" }
        return findMethod(env) {
            name = "isAnnotationPresent"
            returnType = PrimitiveType.BOOLEAN
            parameterTypes += Type.get("java.lang.Class")
        }.callBoolean(env, this) {
            put(find(env, type))
        }
    }

    fun getAnnotation(env: JNIEnvVar, type: Type): JvmObject {
        require(type is ClassType) { "Annotation must be class type" }
        return findMethod(env) {
            name = "getAnnotation"
            returnType = type
        }.callObject(env, this) {
            put(find(env, type))
        }
    }
}