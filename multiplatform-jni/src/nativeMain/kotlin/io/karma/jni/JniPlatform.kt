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

import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.value
import jni.JNI_VERSION_1_8
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import kotlin.concurrent.AtomicNativePtr
import kotlin.concurrent.AtomicReference
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.NativePtr

object JniPlatform {
    private val vmAddress: AtomicNativePtr = AtomicNativePtr(NativePtr.NULL)
    var vm: JavaVm?
        get() = interpretCPointer<JavaVm>(vmAddress.value)?.pointed
        set(value) {
            vmAddress.value = value?.rawPtr ?: NativePtr.NULL
        }
    val environment: ThreadLocalRef<JniEnvironment?> = ThreadLocalRef()
    var loadCallback: AtomicReference<JniScope.() -> Unit> =
        AtomicReference {}
    var unloadCallback: AtomicReference<JniScope.() -> Unit> =
        AtomicReference {}

    inline fun onLoad(crossinline closure: JniScope.() -> Unit) {
        val previousLoadCallback = loadCallback.value
        loadCallback.value = {
            previousLoadCallback()
            closure()
        }
    }

    inline fun onUnload(crossinline closure: JniScope.() -> Unit) {
        val previousLoadCallback = unloadCallback.value
        unloadCallback.value = {
            previousLoadCallback()
            closure()
        }
    }

    fun attach(): JniEnvironment? {
        return if (environment.value != null) environment.value
        else memScoped {
            val envAddress = allocPointerTo<JniEnvironment>()
            if (vm?.pointed?.AttachCurrentThread?.invoke(
                    vm?.ptr,
                    envAddress.ptr.reinterpret(),
                    null
                ) != JNI_OK
            ) return@memScoped null
            environment.value = envAddress.pointed
            environment.value
        }
    }

    fun detach() {
        if (environment.value == null) return
        vm?.pointed?.DetachCurrentThread?.invoke(vm?.ptr)
        environment.value = null
    }
}

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("JNI_OnLoad")
fun jniOnLoad(vm: JavaVm, reserved: COpaquePointer): JvmInt {
    JniPlatform.let { platform ->
        platform.vm = vm
        return vm.pointed?.let {
            memScoped {
                val address = allocPointerTo<JniEnvironment>()
                it.GetEnv?.invoke(
                    vm.ptr,
                    address.ptr.reinterpret(),
                    JNI_VERSION_1_8
                )
                platform.environment.value = address.pointed
                jniScoped { JniPlatform.loadCallback.value(this) }
            }
            JNI_VERSION_1_8
        } ?: JNI_ERR
    }
}

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("JNI_OnUnload")
fun jniOnUnload(vm: JavaVm, reserved: COpaquePointer) {
    jniScoped { JniPlatform.unloadCallback.value(this) }
}

fun MemScope.allocCString(value: String): CPointer<ByteVar> {
    return value.encodeToByteArray().let { bytes ->
        val data = allocArray<ByteVar>(bytes.size)
        bytes.usePinned { memcpy(data, it.addressOf(0), bytes.size.convert()) }
        data
    }
}