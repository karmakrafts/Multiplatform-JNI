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

import jni.JNI_CreateJavaVM
import jni.JNI_GetCreatedJavaVMs
import jni.JNI_VERSION_1_8
import jni.JavaVMInitArgs
import jni.jsizeVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.get
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value

class VmArgument(
    val value: String,
    val extra: COpaquePointer? = null
)

class VmArgsBuilder @InternalJniApi constructor() {
    val options: ArrayList<VmArgument> = ArrayList()
    var ignoreUnrecognized: Boolean = false

    fun onExit(address: CPointer<CFunction<() -> Unit>>) {
        options += VmArgument("exit", address)
    }

    fun onAbort(address: CPointer<CFunction<() -> Unit>>) {
        options += VmArgument("abort", address)
    }
}

value class VirtualMachine private constructor(
    @property:UnsafeJniApi val handle: VmHandle?
) {
    companion object {
        val NULL: VirtualMachine = VirtualMachine(null)

        @UnsafeJniApi
        fun fromHandle(handle: VmHandle?): VirtualMachine {
            return if (handle == null) NULL
            else VirtualMachine(handle)
        }

        @OptIn(UnsafeJniApi::class, InternalJniApi::class)
        inline fun create(closure: VmArgsBuilder.() -> Unit): VirtualMachine = memScoped {
            val builder = VmArgsBuilder().apply(closure)
            val vmAdress = allocPointerTo<VmHandle>()
            val result = JNI_CreateJavaVM(vmAdress.ptr, null, alloc<JavaVMInitArgs> {
                version = JNI_VERSION_1_8
                nOptions = builder.options.size
                options = allocArray(nOptions) { index ->
                    builder.options[index].apply {
                        optionString = allocCString(value)
                        extraInfo = extra
                    }
                }
                ignoreUnrecognized = builder.ignoreUnrecognized.toJBoolean()
            }.ptr)
            if (result != JNI_OK) NULL
            fromHandle(vmAdress.pointed)
        }

        @OptIn(UnsafeJniApi::class)
        fun list(): Array<VirtualMachine> = memScoped {
            val firstVm = allocPointerTo<VmHandle>()
            val numVms = alloc<jsizeVar>()
            if (JNI_GetCreatedJavaVMs(firstVm.ptr, 1, numVms.ptr) != JNI_OK) return emptyArray()
            if (numVms.value == 1) return arrayOf(fromHandle(firstVm.pointed))
            val otherVms = allocArray<CPointerVar<VmHandle>>(numVms.value)
            if (JNI_GetCreatedJavaVMs(otherVms, numVms.value, null) != JNI_OK) return emptyArray()
            Array(numVms.value) { fromHandle(otherVms[it]?.pointed) }
        }
    }

    @OptIn(UnsafeJniApi::class)
    inline val environment: JniEnvironment?
        get() = memScoped {
            val address = allocPointerTo<JniEnvironment>()
            handle?.pointed?.GetEnv?.invoke(handle.ptr, interpretCPointer(address.rawPtr), 0)
            address.pointed
        }

    @OptIn(UnsafeJniApi::class)
    fun destroy() {
        handle?.pointed?.DestroyJavaVM?.invoke(handle.ptr)
    }
}