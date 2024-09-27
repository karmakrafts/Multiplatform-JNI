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

import jni.jbooleanVar
import jni.jbyteVar
import jni.jcharVar
import jni.jdoubleVar
import jni.jfloatVar
import jni.jintVar
import jni.jlongVar
import jni.jshortVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlin.experimental.ExperimentalNativeApi

value class JvmArray private constructor(
    override val handle: JvmArrayHandle?
) : JvmObject {
    companion object {
        val NULL: JvmArray = JvmArray(null)

        fun fromHandle(handle: JvmArrayHandle?): JvmArray {
            return if (handle == null) NULL
            else JvmArray(handle)
        }

        fun fromUnchecked(obj: JvmObject): JvmArray = fromHandle(obj.handle)
    }

    fun getLength(env: JniEnvironment): Int =
        env.pointed?.GetArrayLength?.invoke(env.ptr, handle) ?: 0

    fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        typeClass.componentTypeClass
    }

    // Setters

    fun setByte(env: JniEnvironment, index: Int, value: Byte) {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        address?.reinterpret<jbyteVar>()?.set(index, value)
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
    }

    fun setShort(env: JniEnvironment, index: Int, value: Short) {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        address?.reinterpret<jshortVar>()?.set(index, value)
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
    }

    fun setInt(env: JniEnvironment, index: Int, value: Int) {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        address?.reinterpret<jintVar>()?.set(index, value)
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
    }

    fun setLong(env: JniEnvironment, index: Int, value: Long) {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        address?.reinterpret<jlongVar>()?.set(index, value)
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
    }

    fun setFloat(env: JniEnvironment, index: Int, value: Float) {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        address?.reinterpret<jfloatVar>()?.set(index, value)
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
    }

    fun setDouble(env: JniEnvironment, index: Int, value: Double) {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        address?.reinterpret<jdoubleVar>()?.set(index, value)
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
    }

    fun setBoolean(env: JniEnvironment, index: Int, value: Boolean) {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        address?.reinterpret<jbooleanVar>()?.set(index, value.toJBoolean())
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
    }

    fun setChar(env: JniEnvironment, index: Int, value: Char) {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        address?.reinterpret<jcharVar>()?.set(index, value.code.toUShort())
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
    }

    fun setObject(env: JniEnvironment, index: Int, value: JvmObject) {
        env.pointed?.SetObjectArrayElement?.invoke(env.ptr, handle, index, value.handle)
    }

    inline operator fun <reified R> set(env: JniEnvironment, index: Int, value: R) {
        when (R::class) {
            Byte::class -> setByte(env, index, value as Byte)
            Short::class -> setShort(env, index, value as Short)
            Int::class -> setInt(env, index, value as Int)
            Long::class -> setLong(env, index, value as Long)
            Float::class -> setFloat(env, index, value as Float)
            Double::class -> setDouble(env, index, value as Double)
            Boolean::class -> setBoolean(env, index, value as Boolean)
            Char::class -> setChar(env, index, value as Char)
            JvmObject::class, JvmString::class, JvmClass::class, JvmArray::class -> setObject(
                env,
                index,
                value as JvmObject
            )

            else -> throw IllegalArgumentException("Unsupported array component type")
        }
    }

    // Getters

    fun getByte(env: JniEnvironment, index: Int): Byte {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        val value = address?.reinterpret<jbyteVar>()?.get(index) ?: 0
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
        return value
    }

    fun getShort(env: JniEnvironment, index: Int): Short {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        val value = address?.reinterpret<jshortVar>()?.get(index) ?: 0
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
        return value
    }

    fun getInt(env: JniEnvironment, index: Int): Int {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        val value = address?.reinterpret<jintVar>()?.get(index) ?: 0
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
        return value
    }

    fun getLong(env: JniEnvironment, index: Int): Long {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        val value = address?.reinterpret<jlongVar>()?.get(index) ?: 0
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
        return value
    }

    fun getFloat(env: JniEnvironment, index: Int): Float {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        val value = address?.reinterpret<jfloatVar>()?.get(index) ?: 0F
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
        return value
    }

    fun getDouble(env: JniEnvironment, index: Int): Double {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        val value = address?.reinterpret<jdoubleVar>()?.get(index) ?: 0.0
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
        return value
    }

    fun getBoolean(env: JniEnvironment, index: Int): Boolean {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        val value = address?.reinterpret<jbooleanVar>()?.get(index)?.toKBoolean() ?: false
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
        return value
    }

    @OptIn(ExperimentalNativeApi::class)
    fun getChar(env: JniEnvironment, index: Int): Char {
        val address = env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, handle, null)
        val value = Char.toChars(address?.reinterpret<jcharVar>()?.get(index)?.toInt() ?: 0)[0]
        env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, handle, address, 0)
        return value
    }

    fun getObject(env: JniEnvironment, index: Int): JvmObject {
        return JvmObject.fromHandle(
            env.pointed?.GetObjectArrayElement?.invoke(
                env.ptr,
                handle,
                index
            )
        )
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline operator fun <reified R> get(env: JniEnvironment, index: Int): R = jniScoped(env) {
        when (R::class) {
            Byte::class -> getByte(env, index)
            Short::class -> getShort(env, index)
            Int::class -> getInt(env, index)
            Long::class -> getLong(env, index)
            Float::class -> getFloat(env, index)
            Double::class -> getDouble(env, index)
            Boolean::class -> getBoolean(env, index)
            Char::class -> getChar(env, index)
            JvmObject::class -> getObject(env, index)
            JvmString::class -> getObject(env, index).cast<JvmString>()
            JvmClass::class -> getObject(env, index).cast<JvmClass>()
            JvmArray::class -> getObject(env, index).cast<JvmArray>()
            else -> throw IllegalArgumentException("Unsupported array component type")
        } as R
    }
}