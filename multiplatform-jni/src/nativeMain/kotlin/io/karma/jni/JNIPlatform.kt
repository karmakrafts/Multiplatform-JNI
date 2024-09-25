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

@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)

package io.karma.jni

import jni.JNIEnvVar
import jni.JNI_ERR
import jni.JNI_VERSION_1_8
import jni.JavaVMVar
import jni.jint
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlin.concurrent.AtomicNativePtr
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ThreadLocal
import kotlin.native.internal.NativePtr

private val virtualMachineAddress: AtomicNativePtr = AtomicNativePtr(NativePtr.NULL)
var virtualMachine: JavaVMVar?
    get() = interpretCPointer<JavaVMVar>(virtualMachineAddress.value)?.pointed
    set(value) {
        virtualMachineAddress.value = value?.rawPtr ?: NativePtr.NULL
    }

@ThreadLocal
var environment: JNIEnvVar? = null

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("JNI_OnLoad")
fun jniOnLoad(vm: JavaVMVar, reserved: COpaquePointer): jint {
    virtualMachine = vm
    return vm.pointed?.let {
        memScoped {
            val address = allocPointerTo<JNIEnvVar>()
            requireNotNull(it.GetEnv)(
                vm.ptr,
                address.ptr.reinterpret(),
                JNI_VERSION_1_8
            )
            environment = requireNotNull(address.pointed)
        }
        return@let JNI_VERSION_1_8
    } ?: JNI_ERR
}