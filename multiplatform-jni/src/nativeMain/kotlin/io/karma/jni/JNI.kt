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

fun jstring.toKStringOrNull(env: JNIEnvVar): String? {
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

fun jstring.toKString(env: JNIEnvVar): String =
    requireNotNull(toKStringOrNull(env)) { "Could not convert JVM to native string" }

fun String.toJStringOrNull(env: JNIEnvVar): jstring? {
    return memScoped {
        env.pointed?.NewStringUTF?.invoke(
            env.ptr,
            allocCString(this@toJStringOrNull)
        )
    }
}

fun String.toJString(env: JNIEnvVar): jstring =
    requireNotNull(toJStringOrNull(env)) { "Could not convert native to JVM string" }

fun findClassOrNull(env: JNIEnvVar, name: ClassName): jclass? {
    return memScoped {
        env.pointed?.FindClass?.invoke(env.ptr, allocCString(name.jvmName))
    }
}

fun findClass(env: JNIEnvVar, name: ClassName): jclass =
    requireNotNull(findClassOrNull(env, name)) { "Could not find JVM class" }

fun jclass.findMethodOrNull(env: JNIEnvVar, descriptor: MethodDescriptor): jmethodID? {
    return memScoped {
        if (descriptor.isStatic) env.pointed?.GetStaticMethodID?.invoke(
            env.ptr,
            this@findMethodOrNull,
            allocCString(descriptor.name),
            allocCString(descriptor.jvmDescriptor)
        )
        else env.pointed?.GetMethodID?.invoke(
            env.ptr,
            this@findMethodOrNull,
            allocCString(descriptor.name),
            allocCString(descriptor.jvmDescriptor)
        )
    }
}

fun jclass.findMethodOrNull(
    env: JNIEnvVar,
    descriptor: MethodDescriptorBuilder.() -> Unit
): jmethodID? {
    return findMethodOrNull(env, MethodDescriptor.create(descriptor))
}

fun jclass.findMethod(env: JNIEnvVar, descriptor: MethodDescriptor): jmethodID =
    requireNotNull(findMethodOrNull(env, descriptor)) { "Could not find method" }

fun jclass.findMethod(env: JNIEnvVar, descriptor: MethodDescriptorBuilder.() -> Unit): jmethodID =
    requireNotNull(findMethodOrNull(env, descriptor)) { "Could not find method" }

fun jclass.findFieldOrNull(env: JNIEnvVar, descriptor: FieldDescriptor): jfieldID? {
    return memScoped {
        if (descriptor.isStatic) env.pointed?.GetStaticFieldID?.invoke(
            env.ptr,
            this@findFieldOrNull,
            allocCString(descriptor.name),
            allocCString(descriptor.jvmDescriptor)
        )
        else env.pointed?.GetFieldID?.invoke(
            env.ptr,
            this@findFieldOrNull,
            allocCString(descriptor.name),
            allocCString(descriptor.jvmDescriptor)
        )
    }
}

fun jclass.findFieldOrNull(
    env: JNIEnvVar,
    descriptor: FieldDescriptorBuilder.() -> Unit
): jfieldID? {
    return findFieldOrNull(env, FieldDescriptor.create(descriptor))
}

fun jclass.findField(env: JNIEnvVar, descriptor: FieldDescriptor): jfieldID =
    requireNotNull(findFieldOrNull(env, descriptor)) { "Could not find field" }

fun jclass.findField(env: JNIEnvVar, descriptor: FieldDescriptorBuilder.() -> Unit): jfieldID =
    requireNotNull(findFieldOrNull(env, descriptor)) { "Could not find field" }

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