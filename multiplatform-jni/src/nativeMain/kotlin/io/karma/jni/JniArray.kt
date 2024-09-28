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
fun JvmArrayHandle.pin(env: JniEnvironment): COpaquePointer {
    return requireNotNull(env.pointed?.GetPrimitiveArrayCritical?.invoke(env.ptr, this, null)) {
        "Could not obtained address of pinned JVM array"
    }
}

@UnsafeJniApi
fun JvmArrayHandle.unpin(env: JniEnvironment, address: COpaquePointer) {
    env.pointed?.ReleasePrimitiveArrayCritical?.invoke(env.ptr, this, address, 0)
}

@UnsafeJniApi
inline fun <reified R> JvmArrayHandle.usePinned(
    env: JniEnvironment,
    closure: (COpaquePointer) -> R
): R {
    val address = pin(env)
    val result = closure(address)
    unpin(env, address)
    return result
}

@UnsafeJniApi
inline fun <reified T : CPointed, reified R> JvmArrayHandle.usePinned(
    env: JniEnvironment,
    closure: (CPointer<T>) -> R
): R {
    val address = pin(env)
    val result = closure(address.reinterpret())
    unpin(env, address)
    return result
}

interface JvmArray : JvmObject {
    companion object {
        @UnsafeJniApi
        inline fun <reified T : CVariable> JvmArray.copyPrimitiveDataFrom(
            env: JniEnvironment,
            from: COpaquePointer,
            range: IntRange
        ) = copyPrimitiveDataFrom(env, from, sizeOf<T>().toInt(), range)

        @UnsafeJniApi
        inline fun <reified T : CVariable> JvmArray.copyPrimitiveDataTo(
            env: JniEnvironment,
            to: COpaquePointer,
            range: IntRange
        ) = copyPrimitiveDataTo(env, to, sizeOf<T>().toInt(), range)

        @UnsafeJniApi
        inline fun <reified R> JvmArray.usePinned(
            env: JniEnvironment,
            closure: (COpaquePointer) -> R
        ): R {
            val address = pin(env)
            val result = closure(address)
            unpin(env, address)
            return result
        }

        @UnsafeJniApi
        inline fun <reified T : CPointed, reified R> JvmArray.usePinned(
            env: JniEnvironment,
            closure: (CPointer<T>) -> R
        ): R {
            val address = pin(env)
            val result = closure(address.reinterpret())
            unpin(env, address)
            return result
        }
    }

    fun getLength(env: JniEnvironment): Int

    fun getComponentTypeClass(env: JniEnvironment): JvmClass

    @UnsafeJniApi
    fun pin(env: JniEnvironment): COpaquePointer

    @UnsafeJniApi
    fun unpin(env: JniEnvironment, address: COpaquePointer)

    @UnsafeJniApi
    fun copyPrimitiveDataFrom(
        env: JniEnvironment,
        from: COpaquePointer,
        elementSize: Int,
        range: IntRange
    )

    @UnsafeJniApi
    fun copyPrimitiveDataTo(
        env: JniEnvironment,
        to: COpaquePointer,
        elementSize: Int,
        range: IntRange
    )
}

value class JvmGenericArray private constructor(
    override val handle: JvmArrayHandle?
) : JvmArray {
    companion object {
        val NULL: JvmGenericArray = JvmGenericArray(null)

        @UnsafeJniApi
        fun fromHandle(handle: JvmArrayHandle?): JvmGenericArray {
            return if (handle == null) NULL
            else JvmGenericArray(handle)
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmGenericArray = fromHandle(obj.handle)
    }

    override fun getLength(env: JniEnvironment): Int =
        env.pointed?.GetArrayLength?.invoke(env.ptr, handle) ?: 0

    override fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        typeClass.componentTypeClass
    }

    @UnsafeJniApi
    override fun pin(env: JniEnvironment): COpaquePointer {
        return requireNotNull(handle?.pin(env)) { "Could not pin array object" }
    }

    @UnsafeJniApi
    override fun unpin(env: JniEnvironment, address: COpaquePointer) {
        handle?.unpin(env, address)
    }

    @UnsafeJniApi
    override fun copyPrimitiveDataFrom(
        env: JniEnvironment,
        from: COpaquePointer,
        elementSize: Int,
        range: IntRange
    ): Unit = jniScoped(env) {
        handle?.usePinned {
            memcpy(
                interpretCPointer<COpaque>(it.rawValue + range.first.toLong()),
                from,
                (elementSize * (range.last - range.first)).convert()
            )
        }
    }

    @UnsafeJniApi
    override fun copyPrimitiveDataTo(
        env: JniEnvironment,
        to: COpaquePointer,
        elementSize: Int,
        range: IntRange
    ): Unit = jniScoped(env) {
        handle?.usePinned {
            memcpy(
                to,
                interpretCPointer<COpaque>(it.rawValue + range.first.toLong()),
                (elementSize * (range.last - range.first)).convert()
            )
        }
    }

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
            JvmObject::class, JvmString::class, JvmClass::class, JvmArray::class, JvmGenericArray::class,
            JvmByteArray::class, JvmShortArray::class, JvmIntArray::class, JvmLongArray::class,
            JvmFloatArray::class, JvmDoubleArray::class, JvmBooleanArray::class, JvmCharArray::class,
            JvmObjectArray::class -> setObject(
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

    @OptIn(UnsafeJniApi::class)
    fun getObject(env: JniEnvironment, index: Int): JvmObject {
        return JvmObject.fromHandle(
            env.pointed?.GetObjectArrayElement?.invoke(
                env.ptr,
                handle,
                index
            )
        )
    }

    @OptIn(UnsafeJniApi::class)
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
            JvmString::class -> JvmString.fromUnchecked(getObject(env, index))
            JvmClass::class -> JvmClass.fromUnchecked(getObject(env, index))
            JvmArray::class, JvmGenericArray::class -> fromUnchecked(getObject(env, index))
            JvmByteArray::class -> JvmByteArray.fromUnchecked(getObject(env, index))
            JvmShortArray::class -> JvmShortArray.fromUnchecked(getObject(env, index))
            JvmIntArray::class -> JvmIntArray.fromUnchecked(getObject(env, index))
            JvmLongArray::class -> JvmLongArray.fromUnchecked(getObject(env, index))
            JvmFloatArray::class -> JvmFloatArray.fromUnchecked(getObject(env, index))
            JvmDoubleArray::class -> JvmDoubleArray.fromUnchecked(getObject(env, index))
            JvmBooleanArray::class -> JvmBooleanArray.fromUnchecked(getObject(env, index))
            JvmCharArray::class -> JvmCharArray.fromUnchecked(getObject(env, index))
            JvmObjectArray::class -> JvmObjectArray.fromUnchecked(getObject(env, index))
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

value class JvmByteArray private constructor(
    private val delegate: JvmGenericArray
) : JvmArray by delegate {
    companion object {
        val NULL: JvmByteArray = JvmByteArray(JvmGenericArray.NULL)

        @UnsafeJniApi
        fun fromHandle(handle: JvmByteArrayHandle?): JvmByteArray {
            return if (handle == null) NULL
            else JvmByteArray(JvmGenericArray.fromHandle(handle))
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmByteArray {
            return if (obj.isNull()) NULL
            else JvmByteArray(JvmGenericArray.fromUnchecked(obj))
        }
    }

    override fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        JvmClass.find(PrimitiveType.BYTE)
    }

    @UnsafeJniApi
    fun copyDataFrom(env: JniEnvironment, from: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataFrom(env, from, sizeOf<jbyteVar>().toInt(), range)

    @UnsafeJniApi
    fun copyDataTo(env: JniEnvironment, to: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataTo(env, to, sizeOf<jbyteVar>().toInt(), range)

    fun toArray(env: JniEnvironment): ByteArray =
        delegate.toByteArray(env)

    operator fun set(env: JniEnvironment, index: Int, value: Byte) =
        delegate.setByte(env, index, value)

    operator fun get(env: JniEnvironment, index: Int): Byte =
        delegate.getByte(env, index)
}

value class JvmShortArray private constructor(
    private val delegate: JvmGenericArray
) : JvmArray by delegate {
    companion object {
        val NULL: JvmShortArray = JvmShortArray(JvmGenericArray.NULL)

        @UnsafeJniApi
        fun fromHandle(handle: JvmShortArrayHandle?): JvmShortArray {
            return if (handle == null) NULL
            else JvmShortArray(JvmGenericArray.fromHandle(handle))
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmShortArray {
            return if (obj.isNull()) NULL
            else JvmShortArray(JvmGenericArray.fromUnchecked(obj))
        }
    }

    override fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        JvmClass.find(PrimitiveType.SHORT)
    }

    @UnsafeJniApi
    fun copyDataFrom(env: JniEnvironment, from: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataFrom(env, from, sizeOf<jshortVar>().toInt(), range)

    @UnsafeJniApi
    fun copyDataTo(env: JniEnvironment, to: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataTo(env, to, sizeOf<jshortVar>().toInt(), range)

    fun toArray(env: JniEnvironment): ShortArray =
        delegate.toShortArray(env)

    operator fun set(env: JniEnvironment, index: Int, value: Short) =
        delegate.setShort(env, index, value)

    operator fun get(env: JniEnvironment, index: Int): Short =
        delegate.getShort(env, index)
}

value class JvmIntArray private constructor(
    private val delegate: JvmGenericArray
) : JvmArray by delegate {
    companion object {
        val NULL: JvmIntArray = JvmIntArray(JvmGenericArray.NULL)

        @UnsafeJniApi
        fun fromHandle(handle: JvmIntArrayHandle?): JvmIntArray {
            return if (handle == null) NULL
            else JvmIntArray(JvmGenericArray.fromHandle(handle))
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmIntArray {
            return if (obj.isNull()) NULL
            else JvmIntArray(JvmGenericArray.fromUnchecked(obj))
        }
    }

    override fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        JvmClass.find(PrimitiveType.INT)
    }

    @UnsafeJniApi
    fun copyDataFrom(env: JniEnvironment, from: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataFrom(env, from, sizeOf<jintVar>().toInt(), range)

    @UnsafeJniApi
    fun copyDataTo(env: JniEnvironment, to: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataTo(env, to, sizeOf<jintVar>().toInt(), range)

    fun toArray(env: JniEnvironment): IntArray =
        delegate.toIntArray(env)

    operator fun set(env: JniEnvironment, index: Int, value: Int) =
        delegate.setInt(env, index, value)

    operator fun get(env: JniEnvironment, index: Int): Int =
        delegate.getInt(env, index)
}

value class JvmLongArray private constructor(
    private val delegate: JvmGenericArray
) : JvmArray by delegate {
    companion object {
        val NULL: JvmLongArray = JvmLongArray(JvmGenericArray.NULL)

        @UnsafeJniApi
        fun fromHandle(handle: JvmLongArrayHandle?): JvmLongArray {
            return if (handle == null) NULL
            else JvmLongArray(JvmGenericArray.fromHandle(handle))
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmLongArray {
            return if (obj.isNull()) NULL
            else JvmLongArray(JvmGenericArray.fromUnchecked(obj))
        }
    }

    override fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        JvmClass.find(PrimitiveType.LONG)
    }

    @UnsafeJniApi
    fun copyDataFrom(env: JniEnvironment, from: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataFrom(env, from, sizeOf<jlongVar>().toInt(), range)

    @UnsafeJniApi
    fun copyDataTo(env: JniEnvironment, to: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataTo(env, to, sizeOf<jlongVar>().toInt(), range)

    fun toArray(env: JniEnvironment): LongArray =
        delegate.toLongArray(env)

    operator fun set(env: JniEnvironment, index: Int, value: Long) =
        delegate.setLong(env, index, value)

    operator fun get(env: JniEnvironment, index: Int): Long =
        delegate.getLong(env, index)
}

value class JvmFloatArray private constructor(
    private val delegate: JvmGenericArray
) : JvmArray by delegate {
    companion object {
        val NULL: JvmFloatArray = JvmFloatArray(JvmGenericArray.NULL)

        @UnsafeJniApi
        fun fromHandle(handle: JvmFloatArrayHandle?): JvmFloatArray {
            return if (handle == null) NULL
            else JvmFloatArray(JvmGenericArray.fromHandle(handle))
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmFloatArray {
            return if (obj.isNull()) NULL
            else JvmFloatArray(JvmGenericArray.fromUnchecked(obj))
        }
    }

    override fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        JvmClass.find(PrimitiveType.FLOAT)
    }

    @UnsafeJniApi
    fun copyDataFrom(env: JniEnvironment, from: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataFrom(env, from, sizeOf<jfloatVar>().toInt(), range)

    @UnsafeJniApi
    fun copyDataTo(env: JniEnvironment, to: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataTo(env, to, sizeOf<jfloatVar>().toInt(), range)

    fun toArray(env: JniEnvironment): FloatArray =
        delegate.toFloatArray(env)

    operator fun set(env: JniEnvironment, index: Int, value: Float) =
        delegate.setFloat(env, index, value)

    operator fun get(env: JniEnvironment, index: Int): Float =
        delegate.getFloat(env, index)
}

value class JvmDoubleArray private constructor(
    private val delegate: JvmGenericArray
) : JvmArray by delegate {
    companion object {
        val NULL: JvmDoubleArray = JvmDoubleArray(JvmGenericArray.NULL)

        @UnsafeJniApi
        fun fromHandle(handle: JvmDoubleArrayHandle?): JvmDoubleArray {
            return if (handle == null) NULL
            else JvmDoubleArray(JvmGenericArray.fromHandle(handle))
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmDoubleArray {
            return if (obj.isNull()) NULL
            else JvmDoubleArray(JvmGenericArray.fromUnchecked(obj))
        }
    }

    override fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        JvmClass.find(PrimitiveType.DOUBLE)
    }

    @UnsafeJniApi
    fun copyDataFrom(env: JniEnvironment, from: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataFrom(env, from, sizeOf<jdoubleVar>().toInt(), range)

    @UnsafeJniApi
    fun copyDataTo(env: JniEnvironment, to: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataTo(env, to, sizeOf<jdoubleVar>().toInt(), range)

    fun toArray(env: JniEnvironment): DoubleArray =
        delegate.toDoubleArray(env)

    operator fun set(env: JniEnvironment, index: Int, value: Double) =
        delegate.setDouble(env, index, value)

    operator fun get(env: JniEnvironment, index: Int): Double =
        delegate.getDouble(env, index)
}

value class JvmBooleanArray private constructor(
    private val delegate: JvmGenericArray
) : JvmArray by delegate {
    companion object {
        val NULL: JvmBooleanArray = JvmBooleanArray(JvmGenericArray.NULL)

        @UnsafeJniApi
        fun fromHandle(handle: JvmBooleanArrayHandle?): JvmBooleanArray {
            return if (handle == null) NULL
            else JvmBooleanArray(JvmGenericArray.fromHandle(handle))
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmBooleanArray {
            return if (obj.isNull()) NULL
            else JvmBooleanArray(JvmGenericArray.fromUnchecked(obj))
        }
    }

    override fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        JvmClass.find(PrimitiveType.BOOLEAN)
    }

    @UnsafeJniApi
    fun copyDataFrom(env: JniEnvironment, from: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataFrom(env, from, sizeOf<jbooleanVar>().toInt(), range)

    @UnsafeJniApi
    fun copyDataTo(env: JniEnvironment, to: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataTo(env, to, sizeOf<jbooleanVar>().toInt(), range)

    fun toArray(env: JniEnvironment): BooleanArray =
        delegate.toBooleanArray(env)

    operator fun set(env: JniEnvironment, index: Int, value: Boolean) =
        delegate.setBoolean(env, index, value)

    operator fun get(env: JniEnvironment, index: Int): Boolean =
        delegate.getBoolean(env, index)
}

value class JvmCharArray private constructor(
    private val delegate: JvmGenericArray
) : JvmArray by delegate {
    companion object {
        val NULL: JvmCharArray = JvmCharArray(JvmGenericArray.NULL)

        @UnsafeJniApi
        fun fromHandle(handle: JvmCharArrayHandle?): JvmCharArray {
            return if (handle == null) NULL
            else JvmCharArray(JvmGenericArray.fromHandle(handle))
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmCharArray {
            return if (obj.isNull()) NULL
            else JvmCharArray(JvmGenericArray.fromUnchecked(obj))
        }
    }

    override fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        JvmClass.find(PrimitiveType.CHAR)
    }

    @UnsafeJniApi
    fun copyDataFrom(env: JniEnvironment, from: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataFrom(env, from, sizeOf<jcharVar>().toInt(), range)

    @UnsafeJniApi
    fun copyDataTo(env: JniEnvironment, to: COpaquePointer, range: IntRange) =
        delegate.copyPrimitiveDataTo(env, to, sizeOf<jcharVar>().toInt(), range)

    fun toArray(env: JniEnvironment): CharArray =
        delegate.toCharArray(env)

    operator fun set(env: JniEnvironment, index: Int, value: Char) =
        delegate.setChar(env, index, value)

    operator fun get(env: JniEnvironment, index: Int): Char =
        delegate.getChar(env, index)
}

value class JvmObjectArray private constructor(
    private val delegate: JvmGenericArray
) : JvmArray by delegate {
    companion object {
        val NULL: JvmObjectArray = JvmObjectArray(JvmGenericArray.NULL)

        @UnsafeJniApi
        fun fromHandle(handle: JvmObjectArrayHandle?): JvmObjectArray {
            return if (handle == null) NULL
            else JvmObjectArray(JvmGenericArray.fromHandle(handle))
        }

        @UnsafeJniApi
        fun fromUnchecked(obj: JvmObject): JvmObjectArray {
            return if (obj.isNull()) NULL
            else JvmObjectArray(JvmGenericArray.fromUnchecked(obj))
        }
    }

    override fun getComponentTypeClass(env: JniEnvironment): JvmClass = jniScoped(env) {
        JvmClass.find(Type.OBJECT)
    }

    fun toArray(env: JniEnvironment): Array<JvmObject> =
        delegate.toObjectArray(env)

    operator fun set(env: JniEnvironment, index: Int, value: JvmObject) =
        delegate.setObject(env, index, value)

    operator fun get(env: JniEnvironment, index: Int): JvmObject =
        delegate.getObject(env, index)
}