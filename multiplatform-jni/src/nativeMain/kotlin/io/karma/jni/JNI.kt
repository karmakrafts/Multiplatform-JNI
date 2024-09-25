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
import jni.JNI_FALSE
import jni.JNI_OK
import jni.JNI_TRUE
import jni.jbyte
import jni.jclass
import jni.jfieldID
import jni.jint
import jni.jlong
import jni.jmethodID
import jni.jobject
import jni.jshort
import jni.jstring
import jni.jvalue
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

fun attachThreadToVm(): JNIEnvVar? {
    return if (environment != null) environment
    else memScoped {
        val vm = requireNotNull(virtualMachine?.pointed) { "Could not access virtual machine" }
        val envAddress = allocPointerTo<JNIEnvVar>()
        require(
            requireNotNull(vm.AttachCurrentThread)(
                requireNotNull(virtualMachine?.ptr) { "Could not access virtual machine" },
                envAddress.ptr.reinterpret(),
                null
            ) == JNI_OK
        )
        environment = envAddress.pointed
        environment
    }
}

fun detachThreadFromVm() {
    if (environment == null) return
    val vm = requireNotNull(virtualMachine?.pointed) { "Could not access virtual machine" }
    requireNotNull(vm.DetachCurrentThread) { "Could not access JNI:DetachCurrentThread" }(
        virtualMachine?.ptr
    )
    environment = null
}

fun MemScope.allocCString(value: String): CPointer<ByteVar> {
    return value.encodeToByteArray().let { bytes ->
        val data = allocArray<ByteVar>(bytes.size)
        bytes.usePinned { memcpy(data, it.addressOf(0), bytes.size.convert()) }
        data
    }
}

fun jstring.toKString(env: JNIEnvVar): String {
    val data = requireNotNull(
        requireNotNull(env.pointed?.GetStringUTFChars) { "Could not access JNI:GetStringUTFChars function" }(
            env.ptr,
            this@toKString,
            null
        )
    ) { "Could not retrieve string base address" }
    val result = data.toKString()
    requireNotNull(env.pointed?.ReleaseStringUTFChars) { "Could not access JNI:ReleaseStringUTFChars function" }(
        env.ptr,
        this@toKString,
        data
    )
    return result
}

fun String.toJString(env: JNIEnvVar): jstring {
    return memScoped {
        requireNotNull(
            env.pointed?.NewStringUTF?.invoke(
                env.ptr,
                allocCString(this@toJString)
            )
        ) { "Could not allocate JVM string" }
    }
}

fun jclass.findMethod(env: JNIEnvVar, descriptor: MethodDescriptor): jmethodID? {
    return memScoped {
        if (descriptor.isStatic) env.pointed?.GetStaticMethodID?.invoke(
            env.ptr,
            this@findMethod,
            allocCString(descriptor.name),
            allocCString(descriptor.jvmDescriptor)
        )
        else env.pointed?.GetMethodID?.invoke(
            env.ptr,
            this@findMethod,
            allocCString(descriptor.name),
            allocCString(descriptor.jvmDescriptor)
        )
    }
}

fun jclass.findField(env: JNIEnvVar, descriptor: FieldDescriptor): jfieldID? {
    return memScoped {
        if (descriptor.isStatic) env.pointed?.GetStaticFieldID?.invoke(
            env.ptr,
            this@findField,
            allocCString(descriptor.name),
            allocCString(descriptor.jvmDescriptor)
        )
        else env.pointed?.GetFieldID?.invoke(
            env.ptr,
            this@findField,
            allocCString(descriptor.name),
            allocCString(descriptor.jvmDescriptor)
        )
    }
}

inline operator fun <reified R> jmethodID.invoke(
    env: JNIEnvVar,
    instance: jobject,
    vararg args: Any?
): R? {
    return memScoped {
        val nativeArgs = allocArray<jvalue>(args.size) { index ->
            when (val value = args[index]) {
                is Byte, is UByte -> value as jbyte
                is Short, is UShort -> value as jshort
                is Int, is UInt -> value as jint
                is Long, is ULong -> value as jlong
                is Float -> value
                is Double -> value
                is Boolean -> if (value) JNI_TRUE else JNI_FALSE
                is CPointer<*> -> value.rawValue.toLong().convert<jlong>()
                is COpaquePointer -> value.rawValue.toLong().convert<jlong>()
                null -> null // Null is passed through
                else -> throw IllegalStateException("Unsupported parameter type")
            }
        }
        when (R::class) {
            Byte::class -> env.pointed?.CallByteMethodA?.invoke(
                env.ptr,
                instance,
                this@invoke,
                nativeArgs
            )

            Short::class -> env.pointed?.CallShortMethodA?.invoke(
                env.ptr,
                instance,
                this@invoke,
                nativeArgs
            )

            Int::class -> env.pointed?.CallIntMethodA?.invoke(
                env.ptr,
                instance,
                this@invoke,
                nativeArgs
            )

            Long::class -> env.pointed?.CallLongMethodA?.invoke(
                env.ptr,
                instance,
                this@invoke,
                nativeArgs
            )

            Float::class -> env.pointed?.CallFloatMethodA?.invoke(
                env.ptr,
                instance,
                this@invoke,
                nativeArgs
            )

            Double::class -> env.pointed?.CallDoubleMethodA?.invoke(
                env.ptr,
                instance,
                this@invoke,
                nativeArgs
            )

            Boolean::class -> env.pointed?.CallBooleanMethodA?.invoke(
                env.ptr,
                instance,
                this@invoke,
                nativeArgs
            )

            else -> throw IllegalStateException("Unsupported return type")
        } as? R?
    }
}