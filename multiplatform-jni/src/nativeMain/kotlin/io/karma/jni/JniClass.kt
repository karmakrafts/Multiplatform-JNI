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
import jni.JNIEnvVar
import jni.jclass
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlin.reflect.KClass

interface ClassDescriptor {
    val name: String
    val jvmName: String

    companion object {
        fun fromJvmName(name: String): ClassDescriptor = SimpleClassDescriptor(name.split("/"))
        fun fromName(name: String): ClassDescriptor = SimpleClassDescriptor(name.split("\\."))
        fun of(type: KClass<*>): ClassDescriptor =
            fromName(requireNotNull(type.qualifiedName) { "Could not get type name" })
    }
}

internal data class SimpleClassDescriptor(val segments: List<String>) : ClassDescriptor {
    override val jvmName: String by lazy { segments.joinToString { "/" } }
    override val name: String by lazy { segments.joinToString { "." } }

    override fun toString(): String = name
}

class JvmClass internal constructor(
    val descriptor: ClassDescriptor,
    val handle: jclass
) : ClassDescriptor by descriptor {
    private val fields: ConcurrentMutableMap<FieldDescriptor, JvmField> = ConcurrentMutableMap()
    private val methods: ConcurrentMutableMap<MethodDescriptor, JvmMethod> = ConcurrentMutableMap()

    companion object {
        private val cache: ConcurrentMutableMap<ClassDescriptor, JvmClass> = ConcurrentMutableMap()

        fun findOrNull(env: JNIEnvVar, descriptor: ClassDescriptor): JvmClass? {
            if (descriptor in cache) return cache[descriptor]
            val handle = memScoped {
                env.pointed?.FindClass?.invoke(env.ptr, allocCString(descriptor.jvmName))
            } ?: return null
            return JvmClass(descriptor, handle).apply {
                cache[descriptor] = this
            }
        }

        fun find(env: JNIEnvVar, descriptor: ClassDescriptor): JvmClass =
            requireNotNull(findOrNull(env, descriptor)) { "Could not find class" }
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
            if (descriptor.isStatic) env.pointed?.GetStaticMethodID?.invoke(
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
}