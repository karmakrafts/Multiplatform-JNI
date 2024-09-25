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
import jni.jbooleanVar
import jni.jbyteVar
import jni.jdoubleVar
import jni.jfloatVar
import jni.jintVar
import jni.jlongVar
import jni.jmethodID
import jni.jobjectVar
import jni.jshortVar
import jni.jvalue
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.NativePointed
import kotlinx.cinterop.alloc
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set

interface MethodDescriptor {
    companion object {
        fun create(closure: MethodDescriptorBuilder.() -> Unit): MethodDescriptor {
            return MethodDescriptorBuilder().apply(closure).build()
        }
    }

    val name: String
    val returnType: Type
    val parameterTypes: List<Type>
    val isStatic: Boolean
    val jvmDescriptor: String

    fun getParametersSize(): Int = parameterTypes.sumOf { it.size }
}

internal data class SimpleMethodDescriptor(
    override val name: String,
    override val returnType: Type,
    override val parameterTypes: List<Type>,
    override val isStatic: Boolean,
) : MethodDescriptor {
    override val jvmDescriptor: String by lazy {
        StringBuilder().let {
            it.append('(')
            for (type in parameterTypes) it.append(type.jvmDescriptor)
            it.append(')')
            it.append(returnType.jvmDescriptor)
            it.toString()
        }
    }
}

class MethodDescriptorBuilder internal constructor() {
    var name: String = ""
    var returnType: Type = PrimitiveType.VOID
    var isStatic: Boolean = false
    val parameterTypes: ArrayList<Type> = ArrayList()

    fun setFrom(descriptor: MethodDescriptor) {
        name = descriptor.name
        returnType = descriptor.returnType
        isStatic = descriptor.isStatic
        parameterTypes += descriptor.parameterTypes
    }

    internal fun build(): SimpleMethodDescriptor {
        require(name.isNotBlank()) { "Method name must be specified" }
        return SimpleMethodDescriptor(name, returnType, parameterTypes, isStatic)
    }
}

class ArgumentScope(
    val address: NativePointed,
    private val descriptor: MethodDescriptor,
) {
    private var offset: Long = 0

    fun put(value: Byte) {
        interpretCPointer<jbyteVar>(address.rawPtr + offset)?.set(0, value)
        offset += Byte.SIZE_BYTES
    }

    fun put(value: Short) {
        interpretCPointer<jshortVar>(address.rawPtr + offset)?.set(0, value)
        offset += Short.SIZE_BYTES
    }

    fun put(value: Int) {
        interpretCPointer<jintVar>(address.rawPtr + offset)?.set(0, value)
        offset += Int.SIZE_BYTES
    }

    fun put(value: Long) {
        interpretCPointer<jlongVar>(address.rawPtr + offset)?.set(0, value)
        offset += Long.SIZE_BYTES
    }

    fun put(value: Float) {
        interpretCPointer<jfloatVar>(address.rawPtr + offset)?.set(0, value)
        offset += Float.SIZE_BYTES
    }

    fun put(value: Double) {
        interpretCPointer<jdoubleVar>(address.rawPtr + offset)?.set(0, value)
        offset += Double.SIZE_BYTES
    }

    fun put(value: Boolean) {
        interpretCPointer<jbooleanVar>(address.rawPtr + offset)?.set(0, value.toJBoolean())
        offset += Byte.SIZE_BYTES
    }

    fun put(value: JvmObject) {
        interpretCPointer<jobjectVar>(address.rawPtr + offset)?.set(0, value.handle)
        offset += Long.SIZE_BYTES
    }
}

class JvmMethod(
    val enclosingClass: JvmClass,
    val descriptor: MethodDescriptor,
    val handle: jmethodID,
) : MethodDescriptor by descriptor {
    inline fun MemScope.allocArgs(closure: ArgumentScope.() -> Unit): CPointer<jvalue>? {
        return interpretCPointer(
            ArgumentScope(
                alloc(descriptor.getParametersSize()),
                descriptor
            ).apply(closure).address.rawPtr
        )
    }

    inline fun callByte(
        env: JNIEnvVar,
        instance: JvmObject = JvmNull,
        args: ArgumentScope.() -> Unit
    ): Byte {
        return memScoped {
            if (descriptor.isStatic) env.pointed?.CallStaticByteMethodA?.invoke(
                env.ptr,
                enclosingClass.handle,
                handle,
                allocArgs(args)
            ) ?: 0
            else env.pointed?.CallByteMethodA?.invoke(
                env.ptr,
                instance.handle,
                handle,
                allocArgs(args)
            ) ?: 0
        }
    }

    inline fun callShort(
        env: JNIEnvVar,
        instance: JvmObject = JvmNull,
        args: ArgumentScope.() -> Unit
    ): Short {
        return memScoped {
            if (descriptor.isStatic) env.pointed?.CallStaticShortMethodA?.invoke(
                env.ptr,
                enclosingClass.handle,
                handle,
                allocArgs(args)
            ) ?: 0
            else env.pointed?.CallShortMethodA?.invoke(
                env.ptr,
                instance.handle,
                handle,
                allocArgs(args)
            ) ?: 0
        }
    }

    inline fun callInt(
        env: JNIEnvVar,
        instance: JvmObject = JvmNull,
        args: ArgumentScope.() -> Unit
    ): Int {
        return memScoped {
            if (descriptor.isStatic) env.pointed?.CallStaticIntMethodA?.invoke(
                env.ptr,
                enclosingClass.handle,
                handle,
                allocArgs(args)
            ) ?: 0
            else env.pointed?.CallIntMethodA?.invoke(
                env.ptr,
                instance.handle,
                handle,
                allocArgs(args)
            ) ?: 0
        }
    }

    inline fun callLong(
        env: JNIEnvVar,
        instance: JvmObject = JvmNull,
        args: ArgumentScope.() -> Unit
    ): Long {
        return memScoped {
            if (descriptor.isStatic) env.pointed?.CallStaticLongMethodA?.invoke(
                env.ptr,
                enclosingClass.handle,
                handle,
                allocArgs(args)
            ) ?: 0
            else env.pointed?.CallLongMethodA?.invoke(
                env.ptr,
                instance.handle,
                handle,
                allocArgs(args)
            ) ?: 0
        }
    }

    inline fun callFloat(
        env: JNIEnvVar,
        instance: JvmObject = JvmNull,
        args: ArgumentScope.() -> Unit
    ): Float {
        return memScoped {
            if (descriptor.isStatic) env.pointed?.CallStaticFloatMethodA?.invoke(
                env.ptr,
                enclosingClass.handle,
                handle,
                allocArgs(args)
            ) ?: 0F
            else env.pointed?.CallFloatMethodA?.invoke(
                env.ptr,
                instance.handle,
                handle,
                allocArgs(args)
            ) ?: 0F
        }
    }

    inline fun callDouble(
        env: JNIEnvVar,
        instance: JvmObject = JvmNull,
        args: ArgumentScope.() -> Unit
    ): Double {
        return memScoped {
            if (descriptor.isStatic) env.pointed?.CallStaticDoubleMethodA?.invoke(
                env.ptr,
                enclosingClass.handle,
                handle,
                allocArgs(args)
            ) ?: 0.0
            else env.pointed?.CallDoubleMethodA?.invoke(
                env.ptr,
                instance.handle,
                handle,
                allocArgs(args)
            ) ?: 0.0
        }
    }

    inline fun callBoolean(
        env: JNIEnvVar,
        instance: JvmObject = JvmNull,
        args: ArgumentScope.() -> Unit
    ): Boolean {
        return memScoped {
            if (descriptor.isStatic) env.pointed?.CallStaticBooleanMethodA?.invoke(
                env.ptr,
                enclosingClass.handle,
                handle,
                allocArgs(args)
            )?.toKBoolean() ?: false
            else env.pointed?.CallBooleanMethodA?.invoke(
                env.ptr,
                instance.handle,
                handle,
                allocArgs(args)
            )?.toKBoolean() ?: false
        }
    }

    inline fun callObject(
        env: JNIEnvVar,
        instance: JvmObject = JvmNull,
        args: ArgumentScope.() -> Unit
    ): JvmObject {
        return memScoped {
            JvmObject.fromHandle(
                if (descriptor.isStatic) env.pointed?.CallStaticObjectMethodA?.invoke(
                    env.ptr,
                    enclosingClass.handle,
                    handle,
                    allocArgs(args)
                )
                else env.pointed?.CallObjectMethodA?.invoke(
                    env.ptr,
                    instance.handle,
                    handle,
                    allocArgs(args)
                )
            )
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline fun <reified R> call(
        env: JNIEnvVar,
        instance: JvmObject = JvmNull,
        closure: ArgumentScope.() -> Unit
    ): R {
        return when (R::class) {
            Byte::class -> callByte(env, instance, closure)
            Short::class -> callShort(env, instance, closure)
            Int::class -> callInt(env, instance, closure)
            Long::class -> callLong(env, instance, closure)
            Float::class -> callFloat(env, instance, closure)
            Double::class -> callDouble(env, instance, closure)
            Boolean::class -> callBoolean(env, instance, closure)
            JvmObject::class -> callObject(env, instance, closure)
            else -> throw IllegalArgumentException("Unsupported return type")
        } as R
    }
}