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
import jni.JNI_FALSE
import jni.JNI_TRUE
import jni.JNI_VERSION_1_8
import jni.JavaVMVar
import jni.jbyte
import jni.jclass
import jni.jdouble
import jni.jfieldID
import jni.jfloat
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
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import kotlin.concurrent.AtomicNativePtr
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ThreadLocal
import kotlin.native.internal.NativePtr

private val virtualMachineAddress: AtomicNativePtr = AtomicNativePtr(NativePtr.NULL)
var virtualMachine: JavaVMVar
    get() = interpretCPointer<JavaVMVar>(virtualMachineAddress.value)!!.pointed
    set(value) {
        virtualMachineAddress.value = value.rawPtr
    }

@ThreadLocal
var environment: JNIEnvVar = interpretCPointer<JNIEnvVar>(NativePtr.NULL)!!.pointed

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

value class JNIEnvScope(val environment: JNIEnvVar) {
    inline fun jstring.toKString(): String = toKString(environment)
    inline fun String.toJString(): jstring = toJString(environment)

    inline fun jclass.findField(descriptor: FieldDescriptor): jfieldID? =
        findField(environment, descriptor)

    inline fun jclass.findMethod(descriptor: MethodDescriptor): jmethodID? =
        findMethod(environment, descriptor)

    inline operator fun <reified R> jmethodID.invoke(instance: jobject, vararg args: Any?): R? {
        return invoke<R>(environment, instance, *args)
    }
}

inline fun <reified R> jniScoped(scope: JNIEnvScope.() -> R): R {
    return scope(JNIEnvScope(environment))
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