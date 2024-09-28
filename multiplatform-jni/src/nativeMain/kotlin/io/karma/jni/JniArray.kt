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
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CVariable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.posix.memcpy
import kotlin.experimental.ExperimentalNativeApi

@UnsafeJniApi
inline fun <reified T : CPointed> JvmArrayHandle.pin(env: JniEnvironment): CPointer<T> {
    return requireNotNull(env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, this, null)) {
        "Could not obtained address of pinned JVM array"
    }.reinterpret()
}

@UnsafeJniApi
fun JvmArrayHandle.unpin(env: JniEnvironment, address: COpaquePointer) {
    env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, this, address, 0)
}

@UnsafeJniApi
inline fun <reified T : CPointed, reified R> JvmArrayHandle.usePinned(
    env: JniEnvironment,
    closure: (CPointer<T>) -> R
): R {
    val address = pin<T>(env)
    val result = closure(address)
    unpin(env, address)
    return result
}

value class JvmArray private constructor(
    override val handle: JvmArrayHandle?
) : JvmObject {
    companion object {
        val NULL: JvmArray = JvmArray(null)

        fun fromHandle(handle: JvmArrayHandle?): JvmArray {
            return if (handle == null) NULL
            else JvmArray(handle)
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmArray = fromHandle(obj.handle)
    }

    fun getLength(env: JniEnvironment): Int =
        env.pointed?.GetArrayLength?.invoke(env.ptr, handle) ?: 0

    fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        typeClass.componentTypeClass
    }

    @UnsafeJniApi
    fun copyPrimitiveDataFrom(
        env: JniEnvironment,
        from: COpaquePointer,
        elementSize: Int,
        range: IntRange
    ) = jniScoped(env) {
        handle?.usePinned<COpaque, Unit> {
            memcpy(
                interpretCPointer<COpaque>(it.rawValue + range.first.toLong()),
                from,
                (elementSize * (range.last - range.first)).convert()
            )
        }
    }

    @UnsafeJniApi
    inline fun <reified T : CVariable> copyPrimitiveDataFrom(
        env: JniEnvironment,
        from: COpaquePointer,
        range: IntRange
    ) = copyPrimitiveDataFrom(env, from, sizeOf<T>().toInt(), range)

    @UnsafeJniApi
    fun copyPrimitiveDataTo(
        env: JniEnvironment,
        to: COpaquePointer,
        elementSize: Int,
        range: IntRange
    ) = jniScoped(env) {
        handle?.usePinned<COpaque, Unit> {
            memcpy(
                to,
                interpretCPointer<COpaque>(it.rawValue + range.first.toLong()),
                (elementSize * (range.last - range.first)).convert()
            )
        }
    }

    @UnsafeJniApi
    inline fun <reified T : CVariable> copyPrimitiveDataTo(
        env: JniEnvironment,
        to: COpaquePointer,
        range: IntRange
    ) = copyPrimitiveDataTo(env, to, sizeOf<T>().toInt(), range)

    // Setters

    @OptIn(UnsafeJniApi::class)
    fun setByte(env: JniEnvironment, index: Int, value: Byte) = jniScoped(env) {
        handle?.usePinned<jbyteVar, Unit> { it[index] = value }
    }

    @OptIn(UnsafeJniApi::class)
    fun setShort(env: JniEnvironment, index: Int, value: Short) = jniScoped(env) {
        handle?.usePinned<jshortVar, Unit> { it[index] = value }
    }

    @OptIn(UnsafeJniApi::class)
    fun setInt(env: JniEnvironment, index: Int, value: Int) = jniScoped(env) {
        handle?.usePinned<jintVar, Unit> { it[index] = value }
    }

    @OptIn(UnsafeJniApi::class)
    fun setLong(env: JniEnvironment, index: Int, value: Long) = jniScoped(env) {
        handle?.usePinned<jlongVar, Unit> { it[index] = value }
    }

    @OptIn(UnsafeJniApi::class)
    fun setFloat(env: JniEnvironment, index: Int, value: Float) = jniScoped(env) {
        handle?.usePinned<jfloatVar, Unit> { it[index] = value }
    }

    @OptIn(UnsafeJniApi::class)
    fun setDouble(env: JniEnvironment, index: Int, value: Double) = jniScoped(env) {
        handle?.usePinned<jdoubleVar, Unit> { it[index] = value }
    }

    @OptIn(UnsafeJniApi::class)
    fun setBoolean(env: JniEnvironment, index: Int, value: Boolean) = jniScoped(env) {
        handle?.usePinned<jbooleanVar, Unit> { it[index] = value.toJBoolean() }
    }

    @OptIn(UnsafeJniApi::class)
    fun setChar(env: JniEnvironment, index: Int, value: Char) = jniScoped(env) {
        handle?.usePinned<jcharVar, Unit> { it[index] = value.code.toUShort() }
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

    @OptIn(UnsafeJniApi::class)
    fun getByte(env: JniEnvironment, index: Int): Byte = jniScoped(env) {
        handle?.usePinned<jbyteVar, Byte> { it[index] } ?: 0
    }

    @OptIn(UnsafeJniApi::class)
    fun getShort(env: JniEnvironment, index: Int): Short = jniScoped(env) {
        handle?.usePinned<jshortVar, Short> { it[index] } ?: 0
    }

    @OptIn(UnsafeJniApi::class)
    fun getInt(env: JniEnvironment, index: Int): Int = jniScoped(env) {
        handle?.usePinned<jintVar, Int> { it[index] } ?: 0
    }

    @OptIn(UnsafeJniApi::class)
    fun getLong(env: JniEnvironment, index: Int): Long = jniScoped(env) {
        handle?.usePinned<jlongVar, Long> { it[index] } ?: 0
    }

    @OptIn(UnsafeJniApi::class)
    fun getFloat(env: JniEnvironment, index: Int): Float = jniScoped(env) {
        handle?.usePinned<jfloatVar, Float> { it[index] } ?: 0F
    }

    @OptIn(UnsafeJniApi::class)
    fun getDouble(env: JniEnvironment, index: Int): Double = jniScoped(env) {
        handle?.usePinned<jdoubleVar, Double> { it[index] } ?: 0.0
    }

    @OptIn(UnsafeJniApi::class)
    fun getBoolean(env: JniEnvironment, index: Int): Boolean = jniScoped(env) {
        handle?.usePinned<jbooleanVar, Boolean> { it[index].toKBoolean() } ?: false
    }

    @OptIn(UnsafeJniApi::class, ExperimentalNativeApi::class)
    fun getChar(env: JniEnvironment, index: Int): Char = jniScoped(env) {
        handle?.usePinned<jcharVar, Char> { Char.toChars(it[index].toInt())[0] } ?: ' '
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

    // Conversions

    @OptIn(UnsafeJniApi::class)
    fun toByteArray(env: JniEnvironment): ByteArray = jniScoped(env) {
        ByteArray(length).apply {
            usePinned { pinnedArray ->
                copyPrimitiveDataTo<jbyteVar>(pinnedArray.addressOf(0), indices)
            }
        }
    }

    @OptIn(UnsafeJniApi::class)
    fun toShortArray(env: JniEnvironment): ShortArray = jniScoped(env) {
        ShortArray(length).apply {
            usePinned { pinnedArray ->
                copyPrimitiveDataTo<jshortVar>(pinnedArray.addressOf(0), indices)
            }
        }
    }

    @OptIn(UnsafeJniApi::class)
    fun toIntArray(env: JniEnvironment): IntArray = jniScoped(env) {
        IntArray(length).apply {
            usePinned { pinnedArray ->
                copyPrimitiveDataTo<jintVar>(pinnedArray.addressOf(0), indices)
            }
        }
    }

    @OptIn(UnsafeJniApi::class)
    fun toLongArray(env: JniEnvironment): LongArray = jniScoped(env) {
        LongArray(length).apply {
            usePinned { pinnedArray ->
                copyPrimitiveDataTo<jlongVar>(pinnedArray.addressOf(0), indices)
            }
        }
    }

    @OptIn(UnsafeJniApi::class)
    fun toFloatArray(env: JniEnvironment): FloatArray = jniScoped(env) {
        FloatArray(length).apply {
            usePinned { pinnedArray ->
                copyPrimitiveDataTo<jfloatVar>(pinnedArray.addressOf(0), indices)
            }
        }
    }

    @OptIn(UnsafeJniApi::class)
    fun toDoubleArray(env: JniEnvironment): DoubleArray = jniScoped(env) {
        DoubleArray(length).apply {
            usePinned { pinnedArray ->
                copyPrimitiveDataTo<jdoubleVar>(pinnedArray.addressOf(0), indices)
            }
        }
    }

    fun toBooleanArray(env: JniEnvironment): BooleanArray = jniScoped(env) {
        BooleanArray(length) { getBoolean(it) }
    }

    @OptIn(ExperimentalNativeApi::class)
    fun toCharArray(env: JniEnvironment): CharArray = jniScoped(env) {
        CharArray(length) { Char.toChars(getShort(it).toInt())[0] }
    }

    fun toObjectArray(env: JniEnvironment): Array<JvmObject> = jniScoped(env) {
        Array(length) { getObject(it) }
    }
}