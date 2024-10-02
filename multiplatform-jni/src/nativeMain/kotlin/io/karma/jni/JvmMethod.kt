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

import io.karma.jni.JvmObject.Companion.uncheckedCast
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.NativePointed
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import kotlin.experimental.ExperimentalNativeApi

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

        internal fun MemScope.allocNativeMethod(
            env: JniEnvironment,
            descriptor: MethodDescriptor,
            address: COpaquePointer,
        ): CPointer<JvmNativeMethod>? =
            jniScoped(env) {
                allocPointerTo<JvmNativeMethod>().apply {
                    pointed = alloc<JvmNativeMethod> {
                        fnPtr = address
                        name = allocCString(descriptor.name)
                        signature = allocCString(descriptor.jvmDescriptor)
                    }
                }.value
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
    @property:InternalJniApi val address: NativePointed,
    private val descriptor: MethodDescriptor,
) {
    private var index: Int = 0

    @OptIn(InternalJniApi::class)
    fun put(value: Byte) {
        require(descriptor.parameterTypes[index] == PrimitiveType.BYTE) { "Parameter type mismatch" }
        interpretCPointer<JvmValue>(address.rawPtr + index * sizeOf<JvmValue>())?.pointed?.b = value
        index++
    }

    @OptIn(InternalJniApi::class)
    fun put(value: Short) {
        require(descriptor.parameterTypes[index] == PrimitiveType.SHORT) { "Parameter type mismatch" }
        interpretCPointer<JvmValue>(address.rawPtr + index * sizeOf<JvmValue>())?.pointed?.s = value
        index++
    }

    @OptIn(InternalJniApi::class)
    fun put(value: Int) {
        require(descriptor.parameterTypes[index] == PrimitiveType.INT) { "Parameter type mismatch" }
        interpretCPointer<JvmValue>(address.rawPtr + index * sizeOf<JvmValue>())?.pointed?.i = value
        index++
    }

    @OptIn(InternalJniApi::class)
    fun put(value: Long) {
        require(descriptor.parameterTypes[index] == PrimitiveType.LONG) { "Parameter type mismatch" }
        interpretCPointer<JvmValue>(address.rawPtr + index * sizeOf<JvmValue>())?.pointed?.j = value
        index++
    }

    @OptIn(InternalJniApi::class)
    fun put(value: Float) {
        require(descriptor.parameterTypes[index] == PrimitiveType.FLOAT) { "Parameter type mismatch" }
        interpretCPointer<JvmValue>(address.rawPtr + index * sizeOf<JvmValue>())?.pointed?.f = value
        index++
    }

    @OptIn(InternalJniApi::class)
    fun put(value: Double) {
        require(descriptor.parameterTypes[index] == PrimitiveType.DOUBLE) { "Parameter type mismatch" }
        interpretCPointer<JvmValue>(address.rawPtr + index * sizeOf<JvmValue>())?.pointed?.d = value
        index++
    }

    @OptIn(InternalJniApi::class)
    fun put(value: Boolean) {
        require(descriptor.parameterTypes[index] == PrimitiveType.BOOLEAN) { "Parameter type mismatch" }
        interpretCPointer<JvmValue>(address.rawPtr + index * sizeOf<JvmValue>())?.pointed?.z =
            value.toJBoolean()
        index++
    }

    @OptIn(InternalJniApi::class, UnsafeJniApi::class)
    fun put(value: JvmObject) {
        require(descriptor.parameterTypes[index].typeClass == TypeClass.PRIMITIVE) { "Parameter type mismatch" }
        interpretCPointer<JvmValue>(address.rawPtr + index * sizeOf<JvmValue>())?.pointed?.l =
            value.handle
        index++
    }
}

class JvmMethod(
    val enclosingClass: JvmClass,
    val descriptor: MethodDescriptor,
    @property:UnsafeJniApi val id: JvmMethodId,
) : MethodDescriptor by descriptor, VisibilityProvider, AnnotationProvider {
    @InternalJniApi
    inline fun MemScope.allocArgs(closure: ArgumentScope.() -> Unit): CPointer<JvmValue>? {
        return interpretCPointer(
            ArgumentScope(
                alloc(sizeOf<JvmValue>() * descriptor.parameterTypes.size),
                descriptor
            ).apply(closure).address.rawPtr
        )
    }

    @OptIn(UnsafeJniApi::class, InternalJniApi::class)
    inline fun callVoid(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ) = memScoped {
        when (descriptor.callType) { // @formatter:off
            CallType.STATIC -> env.pointed?.CallStaticVoidMethodA?.invoke(
                env.ptr, enclosingClass.handle, id, allocArgs(args)
            )
            CallType.VIRTUAL -> env.pointed?.CallVoidMethodA?.invoke(
                env.ptr, instance.handle, id, allocArgs(args)
            )
            CallType.DIRECT -> env.pointed?.CallNonvirtualVoidMethodA?.invoke(
                env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
            )
        } ?: 0 // @formatter:on
    }

    @OptIn(UnsafeJniApi::class, InternalJniApi::class)
    inline fun callByte(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Byte = memScoped {
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

    @OptIn(UnsafeJniApi::class, InternalJniApi::class)
    inline fun callShort(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Short = memScoped {
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

    @OptIn(InternalJniApi::class, UnsafeJniApi::class)
    inline fun callInt(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Int = memScoped {
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

    @OptIn(UnsafeJniApi::class, InternalJniApi::class)
    inline fun callLong(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Long = memScoped {
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

    @OptIn(UnsafeJniApi::class, InternalJniApi::class)
    inline fun callFloat(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Float = memScoped {
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

    @OptIn(InternalJniApi::class, UnsafeJniApi::class)
    inline fun callDouble(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Double = memScoped {
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

    @OptIn(UnsafeJniApi::class, InternalJniApi::class)
    inline fun callBoolean(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Boolean = memScoped {
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

    @OptIn(ExperimentalNativeApi::class, UnsafeJniApi::class, InternalJniApi::class)
    inline fun callChar(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): Char = memScoped {
        // @formatter:off
        Char.toChars(when (descriptor.callType) {
            CallType.STATIC -> env.pointed?.CallStaticCharMethodA?.invoke(
                env.ptr, enclosingClass.handle, id, allocArgs(args)
            )
            CallType.VIRTUAL -> env.pointed?.CallCharMethodA?.invoke(
                env.ptr, instance.handle, id, allocArgs(args)
            )
            CallType.DIRECT -> env.pointed?.CallNonvirtualCharMethodA?.invoke(
                env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
            )
        }?.toInt() ?: 0)[0]
        // @formatter:on
    }

    @OptIn(UnsafeJniApi::class, InternalJniApi::class)
    inline fun callObject(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): JvmObject = memScoped {
        // @formatter:off
        JvmObject.fromHandle(when (descriptor.callType) {
            CallType.STATIC -> env.pointed?.CallStaticObjectMethodA?.invoke(
                env.ptr, enclosingClass.handle, id, allocArgs(args)
            )
            CallType.VIRTUAL -> env.pointed?.CallObjectMethodA?.invoke(
                env.ptr, instance.handle, id, allocArgs(args)
            )
            CallType.DIRECT -> env.pointed?.CallNonvirtualObjectMethodA?.invoke(
                env.ptr, instance.handle, enclosingClass.handle, id, allocArgs(args)
            )
        })
        // @formatter:on
    }

    @OptIn(UnsafeJniApi::class)
    inline fun <reified R : JvmObject> callObject(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        args: ArgumentScope.() -> Unit = {}
    ): R = callObject(env, instance, args).uncheckedCast<R>()

    @OptIn(UnsafeJniApi::class)
    inline fun <reified R> call(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): R {
        return when (R::class) {
            Unit::class -> callVoid(env, instance, closure)
            Byte::class -> callByte(env, instance, closure)
            Short::class -> callShort(env, instance, closure)
            Int::class -> callInt(env, instance, closure)
            Long::class -> callLong(env, instance, closure)
            Float::class -> callFloat(env, instance, closure)
            Double::class -> callDouble(env, instance, closure)
            Boolean::class -> callBoolean(env, instance, closure)
            Char::class -> callChar(env, instance, closure)
            JvmObject::class -> callObject(env, instance, closure)
            JvmString::class -> JvmString.fromUnchecked(callObject(env, instance, closure))
            JvmClass::class -> JvmClass.fromUnchecked(callObject(env, instance, closure))
            JvmArray::class, JvmGenericArray::class -> JvmGenericArray.fromUnchecked(
                callObject(
                    env,
                    instance,
                    closure
                )
            )

            JvmByteArray::class -> JvmByteArray.fromUnchecked(callObject(env, instance, closure))
            JvmShortArray::class -> JvmShortArray.fromUnchecked(callObject(env, instance, closure))
            JvmIntArray::class -> JvmIntArray.fromUnchecked(callObject(env, instance))
            JvmLongArray::class -> JvmLongArray.fromUnchecked(callObject(env, instance))
            JvmFloatArray::class -> JvmFloatArray.fromUnchecked(callObject(env, instance))
            JvmDoubleArray::class -> JvmDoubleArray.fromUnchecked(callObject(env, instance))
            JvmBooleanArray::class -> JvmBooleanArray.fromUnchecked(callObject(env, instance))
            JvmCharArray::class -> JvmCharArray.fromUnchecked(callObject(env, instance))
            JvmObjectArray::class -> JvmObjectArray.fromUnchecked(callObject(env, instance))
            else -> throw IllegalArgumentException("Unsupported return type")
        } as R
    }

    @OptIn(UnsafeJniApi::class)
    fun getInstance(env: JniEnvironment): JvmObject =
        JvmObject.fromHandle(
            env.pointed?.ToReflectedMethod?.invoke(
                env.ptr,
                enclosingClass.handle,
                id,
                (descriptor.callType == CallType.STATIC).toJBoolean()
            )
        )

    override fun getVisibility(env: JniEnvironment): JvmVisibility = jniScoped(env) {
        JvmClass.find(Type.METHOD).findMethod {
            name = "getModifiers"
            returnType = PrimitiveType.INT
            callType = CallType.DIRECT
        }.callInt(instance).toUShort().let { modifiers ->
            JvmVisibility.entries.find {
                modifiers and it.jvmValue == it.jvmValue
            } ?: JvmVisibility.PRIVATE
        }
    }

    override fun hasAnnotation(env: JniEnvironment, type: Type): Boolean = jniScoped(env) {
        JvmClass.find(Type.METHOD).findMethod {
            name = "isAnnotationPresent"
            returnType = PrimitiveType.BOOLEAN
            parameterTypes += Type.CLASS
            callType = CallType.DIRECT
        }.callBoolean(instance) {
            put(JvmClass.find(type))
        }
    }

    override fun getAnnotation(env: JniEnvironment, type: Type): JvmObject = jniScoped(env) {
        JvmClass.find(Type.METHOD).findMethod {
            name = "getAnnotation"
            returnType = type
            parameterTypes += Type.CLASS
            callType = CallType.DIRECT
        }.callObject(instance) {
            put(JvmClass.find(type))
        }
    }
}