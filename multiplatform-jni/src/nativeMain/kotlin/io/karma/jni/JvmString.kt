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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString

fun JvmStringHandle.toKStringOrNull(env: JniEnvironment): String? {
    val data = env.pointed?.GetStringUTFChars?.invoke(
        env.ptr,
        this@toKStringOrNull,
        null
    )
    val result = data?.toKString()
    env.pointed?.ReleaseStringUTFChars?.invoke(
        env.ptr,
        this@toKStringOrNull,
        data
    )
    return result
}

fun JvmStringHandle.toKString(env: JniEnvironment): String =
    requireNotNull(toKStringOrNull(env)) { "Could not convert JVM to native string" }

fun String.toJStringOrNull(env: JniEnvironment): JvmStringHandle? {
    return memScoped {
        env.pointed?.NewStringUTF?.invoke(
            env.ptr,
            allocCString(this@toJStringOrNull)
        )
    }
}

fun String.toJString(env: JniEnvironment): JvmStringHandle =
    requireNotNull(toJStringOrNull(env)) { "Could not convert native to JVM string" }

value class JvmString @UnsafeJniApi private constructor(
    @property:UnsafeJniApi override val handle: JvmStringHandle?
) : JvmObject {
    companion object {
        @property:OptIn(UnsafeJniApi::class)
        val NULL: JvmString = JvmString(null)

        @UnsafeJniApi
        fun fromHandle(handle: JvmStringHandle?): JvmString {
            return if (handle == null) NULL
            else JvmString(handle)
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmString = fromHandle(obj.handle)

        @OptIn(UnsafeJniApi::class)
        fun create(env: JniEnvironment, value: String): JvmString {
            return memScoped {
                fromHandle(env.pointed?.NewStringUTF?.invoke(env.ptr, allocCString(value)))
            }
        }
    }

    @OptIn(UnsafeJniApi::class)
    fun get(env: JniEnvironment): String? = handle?.toKStringOrNull(env)

    @OptIn(UnsafeJniApi::class)
    fun getLength(env: JniEnvironment): Int =
        env.pointed?.GetStringLength?.invoke(env.ptr, handle) ?: 0
}