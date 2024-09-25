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

import jni.JNIEnvVar
import jni.jfieldID
import jni.jobject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr

interface FieldDescriptor {
    val name: String
    val type: Type
    val isStatic: Boolean
    val jvmDescriptor: String

    companion object {
        fun create(closure: FieldDescriptorBuilder.() -> Unit): FieldDescriptor {
            return FieldDescriptorBuilder().apply(closure).build()
        }
    }
}

class FieldDescriptorBuilder internal constructor() {
    var name: String = ""
    var type: Type? = null
    var isStatic: Boolean = false

    fun setFrom(descriptor: FieldDescriptor) {
        name = descriptor.name
        type = descriptor.type
        isStatic = descriptor.isStatic
    }

    internal fun build(): SimpleFieldDescriptor {
        require(name.isNotBlank()) { "Field name must be specified" }
        require(type != null) { "Field type must be specified" }
        return SimpleFieldDescriptor(name, type!!, isStatic)
    }
}

internal data class SimpleFieldDescriptor(
    override val name: String,
    override val type: Type,
    override val isStatic: Boolean,
) : FieldDescriptor {
    override val jvmDescriptor: String = type.jvmDescriptor

    override fun toString(): String = "$name:$jvmDescriptor"
}

class JvmField(
    val enclosingClass: JvmClass,
    val descriptor: FieldDescriptor,
    val handle: jfieldID,
) : FieldDescriptor by descriptor {
    // Getters

    fun getByte(env: JNIEnvVar, instance: jobject? = null): Byte {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticByteField?.invoke(env.ptr, enclosingClass.handle, handle)
                ?: 0
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetByteField?.invoke(env.ptr, instance, handle) ?: 0
    }

    fun getShort(env: JNIEnvVar, instance: jobject? = null): Short {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticShortField?.invoke(env.ptr, enclosingClass.handle, handle)
                ?: 0
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetShortField?.invoke(env.ptr, instance, handle) ?: 0
    }

    fun getInt(env: JNIEnvVar, instance: jobject? = null): Int {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticIntField?.invoke(env.ptr, enclosingClass.handle, handle)
                ?: 0
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetIntField?.invoke(env.ptr, instance, handle) ?: 0
    }

    fun getLong(env: JNIEnvVar, instance: jobject? = null): Long {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticLongField?.invoke(env.ptr, enclosingClass.handle, handle)
                ?: 0
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetLongField?.invoke(env.ptr, instance, handle) ?: 0
    }

    fun getFloat(env: JNIEnvVar, instance: jobject? = null): Float {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticFloatField?.invoke(env.ptr, enclosingClass.handle, handle)
                ?: 0F
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetFloatField?.invoke(env.ptr, instance, handle) ?: 0F
    }

    fun getDouble(env: JNIEnvVar, instance: jobject? = null): Double {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticDoubleField?.invoke(env.ptr, enclosingClass.handle, handle)
                ?: 0.0
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetDoubleField?.invoke(env.ptr, instance, handle) ?: 0.0
    }

    fun getBoolean(env: JNIEnvVar, instance: jobject? = null): Boolean {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticBooleanField?.invoke(
                env.ptr,
                enclosingClass.handle,
                handle
            )?.toKBoolean() ?: false
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetBooleanField?.invoke(env.ptr, instance, handle)?.toKBoolean()
            ?: false
    }

    // Setters

    fun setByte(env: JNIEnvVar, value: Byte, instance: jobject? = null) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticByteField?.invoke(env.ptr, enclosingClass.handle, handle, value)
            return
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        env.pointed?.SetByteField?.invoke(env.ptr, instance, handle, value.convert())
    }

    fun setShort(env: JNIEnvVar, value: Short, instance: jobject? = null) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticShortField?.invoke(env.ptr, enclosingClass.handle, handle, value)
            return
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        env.pointed?.SetShortField?.invoke(env.ptr, instance, handle, value.convert())
    }

    fun setInt(env: JNIEnvVar, value: Int, instance: jobject? = null) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticIntField?.invoke(env.ptr, enclosingClass.handle, handle, value)
            return
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        env.pointed?.SetIntField?.invoke(env.ptr, instance, handle, value.convert())
    }

    fun setLong(env: JNIEnvVar, value: Long, instance: jobject? = null) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticLongField?.invoke(env.ptr, enclosingClass.handle, handle, value)
            return
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        env.pointed?.SetLongField?.invoke(env.ptr, instance, handle, value.convert())
    }

    fun setFloat(env: JNIEnvVar, value: Float, instance: jobject? = null) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticFloatField?.invoke(env.ptr, enclosingClass.handle, handle, value)
            return
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        env.pointed?.SetFloatField?.invoke(env.ptr, instance, handle, value)
    }

    fun setDouble(env: JNIEnvVar, value: Double, instance: jobject? = null) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticDoubleField?.invoke(env.ptr, enclosingClass.handle, handle, value)
            return
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        env.pointed?.SetDoubleField?.invoke(env.ptr, instance, handle, value)
    }

    fun setBoolean(env: JNIEnvVar, value: Boolean, instance: jobject? = null) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticBooleanField?.invoke(
                env.ptr,
                enclosingClass.handle,
                handle,
                value.toJBoolean()
            )
            return
        }
        require(instance != null) { "Instance must not be null for non-static fields" }
        env.pointed?.SetBooleanField?.invoke(env.ptr, instance, handle, value.toJBoolean())
    }
}