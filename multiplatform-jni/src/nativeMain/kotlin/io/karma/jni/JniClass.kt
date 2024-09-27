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
import io.karma.jni.MethodDescriptor.Companion.allocNativeMethod
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr

class JvmClass internal constructor(
    override val handle: JvmClassHandle?
) : JvmObject, VisibilityProvider, AnnotationProvider {
    internal val fields: ConcurrentMutableMap<FieldDescriptor, JvmField> = ConcurrentMutableMap()
    internal val methods: ConcurrentMutableMap<MethodDescriptor, JvmMethod> = ConcurrentMutableMap()

    companion object {
        val NULL: JvmClass = JvmClass(null)
        private val cache: ConcurrentMutableMap<JvmClassHandle, JvmClass> = ConcurrentMutableMap()

        fun fromHandle(handle: JvmClassHandle?): JvmClass {
            return if (handle == null) NULL
            else cache.getOrPut(handle) { JvmClass(handle) }
        }

        fun fromUnchecked(obj: JvmObject): JvmClass = fromHandle(obj.handle)

        fun findOrNull(env: JniEnvironment, type: Type): JvmClass? {
            val handle = memScoped {
                env.pointed?.FindClass?.invoke(env.ptr, allocCString(type.jvmName))
            } ?: return null
            return if (handle in cache) cache[handle]!!
            else JvmClass(handle).apply {
                cache[handle] = this
            }
        }

        fun find(env: JniEnvironment, type: Type): JvmClass =
            requireNotNull(findOrNull(env, type)) { "Could not find class" }
    }

    fun findFieldOrNull(env: JniEnvironment, descriptor: FieldDescriptor): JvmField? {
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

    fun findFieldOrNull(
        env: JniEnvironment,
        closure: FieldDescriptorBuilder.() -> Unit
    ): JvmField? =
        findFieldOrNull(env, FieldDescriptor.create(closure))

    fun findField(env: JniEnvironment, descriptor: FieldDescriptor): JvmField =
        requireNotNull(findFieldOrNull(env, descriptor)) { "Could not find field" }

    fun findField(env: JniEnvironment, closure: FieldDescriptorBuilder.() -> Unit): JvmField =
        findField(env, FieldDescriptor.create(closure))

    fun findMethodOrNull(env: JniEnvironment, descriptor: MethodDescriptor): JvmMethod? {
        if (descriptor in methods) return methods[descriptor]
        val handle = memScoped {
            if (descriptor.callType == CallType.STATIC) env.pointed?.GetStaticMethodID?.invoke(
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

    fun findMethodOrNull(
        env: JniEnvironment,
        closure: MethodDescriptorBuilder.() -> Unit
    ): JvmMethod? =
        findMethodOrNull(env, MethodDescriptor.create(closure))

    fun findMethod(env: JniEnvironment, descriptor: MethodDescriptor): JvmMethod =
        requireNotNull(findMethodOrNull(env, descriptor)) { "Could not find method" }

    fun findMethod(env: JniEnvironment, closure: MethodDescriptorBuilder.() -> Unit): JvmMethod =
        findMethod(env, MethodDescriptor.create(closure))

    fun getType(env: JniEnvironment): Type = jniScoped(env) {
        findMethod {
            name = "getName"
            returnType = Type.STRING
            callType = CallType.DIRECT
        }.callObject(this@JvmClass)
            .cast<JvmString>()
            .value
            ?.let(Type::get)
            ?: NullType
    }

    override fun hasAnnotation(env: JniEnvironment, type: Type): Boolean = jniScoped(env) {
        require(type.typeClass == TypeClass.OBJECT) { "Annotation must be class type" }
        findMethod {
            name = "isAnnotationPresent"
            returnType = PrimitiveType.BOOLEAN
            parameterTypes += Type.CLASS
            callType = CallType.DIRECT
        }.callBoolean(this@JvmClass) {
            put(find(type))
        }
    }

    override fun getAnnotation(env: JniEnvironment, type: Type): JvmObject = jniScoped(env) {
        require(type.typeClass == TypeClass.OBJECT) { "Annotation must be class type" }
        findMethod {
            name = "getAnnotation"
            returnType = type
            parameterTypes += Type.CLASS
            callType = CallType.DIRECT
        }.callObject(this@JvmClass) {
            put(find(type))
        }
    }

    override fun getVisibility(env: JniEnvironment): JvmVisibility = jniScoped(env) {
        findMethod {
            name = "getModifiers"
            returnType = PrimitiveType.INT
            callType = CallType.DIRECT
        }.callInt(this@JvmClass).toUShort().let { modifiers ->
            JvmVisibility.entries.find {
                (modifiers and it.jvmValue) == it.jvmValue
            } ?: JvmVisibility.PRIVATE
        }
    }

    fun getFields(env: JniEnvironment): List<JvmField> = jniScoped(env) {
        return emptyList() // TODO
    }

    fun getMethods(env: JniEnvironment): List<JvmMethod> = jniScoped(env) {
        return emptyList() // TODO
    }

    fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        findMethod {
            name = "getComponentType"
            returnType = Type.CLASS
        }.callObject().cast()
    }

    fun registerNativeMethod(
        env: JniEnvironment,
        address: COpaquePointer,
        descriptor: MethodDescriptor,
    ) = memScoped {
        env.pointed?.RegisterNatives?.invoke(
            env.ptr,
            handle,
            allocNativeMethod(env, descriptor, address),
            1 // We always register one method at a time
        )
    }

    fun registerNativeMethod(
        env: JniEnvironment,
        address: COpaquePointer,
        closure: MethodDescriptorBuilder.() -> Unit,
    ) = registerNativeMethod(env, address, MethodDescriptor.create(closure))
}