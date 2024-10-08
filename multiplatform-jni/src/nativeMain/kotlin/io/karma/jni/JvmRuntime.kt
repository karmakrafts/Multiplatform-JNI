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

value class JvmRuntime @UnsafeJniApi private constructor(
    @property:UnsafeJniApi override val handle: JvmObjectHandle
) : JvmObject {
    companion object {
        @OptIn(UnsafeJniApi::class)
        fun get(env: JniEnvironment): JvmRuntime = jniScoped(env) {
            JvmClass.find(Type.get("java.lang.Runtime")).findMethod {
                name = "getRuntime"
                returnType = Type.get("java.lang.Runtime")
                callType = CallType.STATIC
            }.callObject().handle.let { JvmRuntime(requireNotNull(it)) }
        }
    }

    fun getAvailableProcessors(env: JniEnvironment): Int = jniScoped(env) {
        JvmClass.find(Type.get("java.lang.Runtime")).findMethod {
            name = "availableProcessors"
            returnType = PrimitiveType.INT
            callType = CallType.DIRECT
        }.callInt(this@JvmRuntime)
    }

    fun getTotalMemory(env: JniEnvironment): Int = jniScoped(env) {
        JvmClass.find(Type.get("java.lang.Runtime")).findMethod {
            name = "totalMemory"
            returnType = PrimitiveType.INT
            callType = CallType.DIRECT
        }.callInt(this@JvmRuntime)
    }

    fun getMaxMemory(env: JniEnvironment): Int = jniScoped(env) {
        JvmClass.find(Type.get("java.lang.Runtime")).findMethod {
            name = "maxMemory"
            returnType = PrimitiveType.INT
            callType = CallType.DIRECT
        }.callInt(this@JvmRuntime)
    }

    fun freeMemory(env: JniEnvironment) = jniScoped(env) {
        JvmClass.find(Type.get("java.lang.Runtime")).findMethod {
            this.name = "freeMemory"
            callType = CallType.DIRECT
        }.callVoid(this@JvmRuntime)
    }

    fun gc(env: JniEnvironment) = jniScoped(env) {
        JvmClass.find(Type.get("java.lang.Runtime")).findMethod {
            this.name = "gc"
            callType = CallType.DIRECT
        }.callVoid(this@JvmRuntime)
    }
}