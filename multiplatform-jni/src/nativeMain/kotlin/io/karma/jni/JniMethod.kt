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
import jni.jmethodID
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
import kotlinx.cinterop.sizeOf

enum class CallType {
    STATIC,
    VIRTUAL,
    DIRECT
}

interface MethodDescriptor {
    companion object {
        fun create(closure: MethodDescriptorBuilder.() -> Unit): MethodDescriptor {
            return MethodDescriptorBuilder().apply(closure).build()
        }
    }

    val name: String
    val returnType: Type
    val parameterTypes: List<Type>
    val callType: CallType
    val jvmDescriptor: String
}

internal data class SimpleMethodDescriptor(
    override val name: String,
    override val returnType: Type,
    override val parameterTypes: List<Type>,
    override val callType: CallType,
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
    var callType: CallType = CallType.VIRTUAL
    val parameterTypes: ArrayList<Type> = ArrayList()

    fun setFrom(descriptor: MethodDescriptor) {
        name = descriptor.name
        returnType = descriptor.returnType
        callType = descriptor.callType
        parameterTypes += descriptor.parameterTypes
    }

    internal fun build(): SimpleMethodDescriptor {
        require(name.isNotBlank()) { "Method name must be specified" }
        return SimpleMethodDescriptor(name, returnType, parameterTypes, callType)
    }
}

class ArgumentScope(
    val address: NativePointed,
    private val descriptor: MethodDescriptor,
) {
    private var index: Int = 0

    fun put(value: Byte) {
        require(descriptor.parameterTypes[index] == PrimitiveType.BYTE) { "Parameter type mismatch" }
        interpretCPointer<jvalue>(address.rawPtr + index * sizeOf<jvalue>())?.pointed?.b = value
        index++
    }

    fun put(value: Short) {
        require(descriptor.parameterTypes[index] == PrimitiveType.SHORT) { "Parameter type mismatch" }
        interpretCPointer<jvalue>(address.rawPtr + index * sizeOf<jvalue>())?.pointed?.s = value
        index++
    }

    fun put(value: Int) {
        require(descriptor.parameterTypes[index] == PrimitiveType.INT) { "Parameter type mismatch" }
        interpretCPointer<jvalue>(address.rawPtr + index * sizeOf<jvalue>())?.pointed?.i = value
        index++
    }

    fun put(value: Long) {
        require(descriptor.parameterTypes[index] == PrimitiveType.LONG) { "Parameter type mismatch" }
        interpretCPointer<jvalue>(address.rawPtr + index * sizeOf<jvalue>())?.pointed?.j = value
        index++
    }

    fun put(value: Float) {
        require(descriptor.parameterTypes[index] == PrimitiveType.FLOAT) { "Parameter type mismatch" }
        interpretCPointer<jvalue>(address.rawPtr + index * sizeOf<jvalue>())?.pointed?.f = value
        index++
    }

    fun put(value: Double) {
        require(descriptor.parameterTypes[index] == PrimitiveType.DOUBLE) { "Parameter type mismatch" }
        interpretCPointer<jvalue>(address.rawPtr + index * sizeOf<jvalue>())?.pointed?.d = value
        index++
    }

    fun put(value: Boolean) {
        require(descriptor.parameterTypes[index] == PrimitiveType.BOOLEAN) { "Parameter type mismatch" }
        interpretCPointer<jvalue>(address.rawPtr + index * sizeOf<jvalue>())?.pointed?.z =
            value.toJBoolean()
        index++
    }

    fun put(value: JvmObject) {
        require(descriptor.parameterTypes[index] !is PrimitiveType) { "Parameter type mismatch" }
        interpretCPointer<jvalue>(address.rawPtr + index * sizeOf<jvalue>())?.pointed?.l =
            value.handle
        index++
    }
}

class JvmMethod(
    val enclosingClass: JvmClass,
    val descriptor: MethodDescriptor,
    val id: jmethodID,
) : MethodDescriptor by descriptor {
    inline fun MemScope.allocArgs(closure: ArgumentScope.() -> Unit): CPointer<jvalue>? {
        return interpretCPointer(
            ArgumentScope(
                alloc(sizeOf<jvalue>() * descriptor.parameterTypes.size),
                descriptor
            ).apply(closure).address.rawPtr
        )
    }

    inline fun callByte(
        env: JNIEnvVar,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Byte {
        return memScoped {
            when (descriptor.callType) { // @formatter:off
                CallType.STATIC -> env.pointed?.CallStaticByteMethodA?.invoke(
                    env.ptr, enclosingClass.handle, id, allocArgs(args)
                )
                CallType.VIRTUAL -> env.pointed?.CallByteMethodA?.invoke(
                    env.ptr, instance.handle, id, allocArgs(args)
                )
                CallType.DIRECT -> env.pointed?.CallNonvirtualByteMethodA?.invoke(
                    env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
                )
            } ?: 0 // @formatter:on
        }
    }

    inline fun callShort(
        env: JNIEnvVar,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Short {
        return memScoped {
            when (descriptor.callType) { // @formatter:off
                CallType.STATIC -> env.pointed?.CallStaticShortMethodA?.invoke(
                    env.ptr, enclosingClass.handle, id, allocArgs(args)
                )
                CallType.VIRTUAL -> env.pointed?.CallShortMethodA?.invoke(
                    env.ptr, instance.handle, id, allocArgs(args)
                )
                CallType.DIRECT -> env.pointed?.CallNonvirtualShortMethodA?.invoke(
                    env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
                )
            } ?: 0 // @formatter:on
        }
    }

    inline fun callInt(
        env: JNIEnvVar,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Int {
        return memScoped {
            when (descriptor.callType) { // @formatter:off
                CallType.STATIC -> env.pointed?.CallStaticIntMethodA?.invoke(
                    env.ptr, enclosingClass.handle, id, allocArgs(args)
                )
                CallType.VIRTUAL -> env.pointed?.CallIntMethodA?.invoke(
                    env.ptr, instance.handle, id, allocArgs(args)
                )
                CallType.DIRECT -> env.pointed?.CallNonvirtualIntMethodA?.invoke(
                    env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
                )
            } ?: 0 // @formatter:on
        }
    }

    inline fun callLong(
        env: JNIEnvVar,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Long {
        return memScoped {
            when (descriptor.callType) { // @formatter:off
                CallType.STATIC -> env.pointed?.CallStaticLongMethodA?.invoke(
                    env.ptr, enclosingClass.handle, id, allocArgs(args)
                )
                CallType.VIRTUAL -> env.pointed?.CallLongMethodA?.invoke(
                    env.ptr, instance.handle, id, allocArgs(args)
                )
                CallType.DIRECT -> env.pointed?.CallNonvirtualLongMethodA?.invoke(
                    env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
                )
            } ?: 0 // @formatter:on
        }
    }

    inline fun callFloat(
        env: JNIEnvVar,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Float {
        return memScoped {
            when (descriptor.callType) { // @formatter:off
                CallType.STATIC -> env.pointed?.CallStaticFloatMethodA?.invoke(
                    env.ptr, enclosingClass.handle, id, allocArgs(args)
                )
                CallType.VIRTUAL -> env.pointed?.CallFloatMethodA?.invoke(
                    env.ptr, instance.handle, id, allocArgs(args)
                )
                CallType.DIRECT -> env.pointed?.CallNonvirtualFloatMethodA?.invoke(
                    env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
                )
            } ?: 0F // @formatter:on
        }
    }

    inline fun callDouble(
        env: JNIEnvVar,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Double {
        return memScoped {
            when (descriptor.callType) { // @formatter:off
                CallType.STATIC -> env.pointed?.CallStaticDoubleMethodA?.invoke(
                    env.ptr, enclosingClass.handle, id, allocArgs(args)
                )
                CallType.VIRTUAL -> env.pointed?.CallDoubleMethodA?.invoke(
                    env.ptr, instance.handle, id, allocArgs(args)
                )
                CallType.DIRECT -> env.pointed?.CallNonvirtualDoubleMethodA?.invoke(
                    env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
                )
            } ?: 0.0 // @formatter:on
        }
    }

    inline fun callBoolean(
        env: JNIEnvVar,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Boolean {
        return memScoped {
            when (descriptor.callType) { // @formatter:off
                CallType.STATIC -> env.pointed?.CallStaticBooleanMethodA?.invoke(
                    env.ptr, enclosingClass.handle, id, allocArgs(args)
                )
                CallType.VIRTUAL -> env.pointed?.CallBooleanMethodA?.invoke(
                    env.ptr, instance.handle, id, allocArgs(args)
                )
                CallType.DIRECT -> env.pointed?.CallNonvirtualBooleanMethodA?.invoke(
                    env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
                )
            }?.toKBoolean() ?: false // @formatter:on
        }
    }

    inline fun callObject(
        env: JNIEnvVar,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): JvmObject {
        return memScoped {
            JvmObject.fromHandle(when (descriptor.callType) { // @formatter:off
                CallType.STATIC -> env.pointed?.CallStaticObjectMethodA?.invoke(
                    env.ptr, enclosingClass.handle, id, allocArgs(args)
                )
                CallType.VIRTUAL -> env.pointed?.CallObjectMethodA?.invoke(
                    env.ptr, instance.handle, id, allocArgs(args)
                )
                CallType.DIRECT -> env.pointed?.CallNonvirtualObjectMethodA?.invoke(
                    env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
                )
            }
            ) // @formatter:on
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline fun <reified R> call(
        env: JNIEnvVar,
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
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

    fun getInstance(env: JNIEnvVar): JvmObject =
        JvmObject.fromHandle(
            env.pointed?.ToReflectedMethod?.invoke(
                env.ptr,
                enclosingClass.handle,
                id,
                (descriptor.callType == CallType.STATIC).toJBoolean()
            )
        )

    fun getVisibility(env: JNIEnvVar): JvmVisibility = jniScoped(env) {
        JvmClass.find(Type.get("java.lang.reflect.Method")).let { methodClass ->
            methodClass.findMethod {
                name = "getModifiers"
                returnType = PrimitiveType.INT
                callType = CallType.DIRECT
            }.callInt(instance).toUShort().let { modifiers ->
                JvmVisibility.entries.find {
                    modifiers and it.jvmValue == it.jvmValue
                } ?: JvmVisibility.PRIVATE
            }
        }
    }
}