@file:OptIn(ExperimentalForeignApi::class)

package io.karma.jni

import jni.JNIEnvVar
import jni.JNI_FALSE
import jni.JNI_TRUE
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
import jni.jvalue
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

fun MemScope.allocCString(value: String): CPointer<ByteVar> {
    return value.encodeToByteArray().let { bytes ->
        val data = allocArray<ByteVar>(bytes.size)
        bytes.usePinned { memcpy(data, it.addressOf(0), bytes.size.convert()) }
        data
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
                is Float -> value as jfloat
                is Double -> value as jdouble
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