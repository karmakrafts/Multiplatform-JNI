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

import kotlinx.cinterop.ExperimentalForeignApi

class JvmByteArrayView internal constructor(
    val array: JvmByteArray,
    private val env: JniEnvironment,
) : JvmArray by array, Iterable<Byte> {
    override fun iterator(): ByteIterator = array.getIterator(env)
}

class JvmShortArrayView internal constructor(
    val array: JvmShortArray,
    private val env: JniEnvironment,
) : JvmArray by array, Iterable<Short> {
    override fun iterator(): ShortIterator = array.getIterator(env)
}

class JvmIntArrayView internal constructor(
    val array: JvmIntArray,
    private val env: JniEnvironment,
) : JvmArray by array, Iterable<Int> {
    override fun iterator(): IntIterator = array.getIterator(env)
}

class JvmLongArrayView internal constructor(
    val array: JvmLongArray,
    private val env: JniEnvironment,
) : JvmArray by array, Iterable<Long> {
    override fun iterator(): LongIterator = array.getIterator(env)
}

class JvmFloatArrayView internal constructor(
    val array: JvmFloatArray,
    private val env: JniEnvironment,
) : JvmArray by array, Iterable<Float> {
    override fun iterator(): FloatIterator = array.getIterator(env)
}

class JvmDoubleArrayView internal constructor(
    val array: JvmDoubleArray,
    private val env: JniEnvironment,
) : JvmArray by array, Iterable<Double> {
    override fun iterator(): DoubleIterator = array.getIterator(env)
}

class JvmBooleanArrayView internal constructor(
    val array: JvmBooleanArray,
    private val env: JniEnvironment,
) : JvmArray by array, Iterable<Boolean> {
    override fun iterator(): BooleanIterator = array.getIterator(env)
}

class JvmCharArrayView internal constructor(
    val array: JvmCharArray,
    private val env: JniEnvironment,
) : JvmArray by array, Iterable<Char> {
    override fun iterator(): CharIterator = array.getIterator(env)
}

class JvmObjectArrayView internal constructor(
    val array: JvmObjectArray,
    private val env: JniEnvironment,
) : JvmArray by array, Iterable<JvmObject> {
    override fun iterator(): Iterator<JvmObject> = array.getIterator(env)
}