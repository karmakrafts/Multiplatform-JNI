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

import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.sizeOf
import kotlin.reflect.KClass

interface Type {
    companion object {
        fun fromName(qualifiedName: String): Type =
            ClassType(ClassDescriptor.fromName(qualifiedName))

        fun of(type: KClass<*>): Type = ClassType(ClassDescriptor.of(type))
        inline fun <reified T> of(): Type = of(T::class)
    }

    val name: String
    val valueType: Type
    val size: Int

    val jvmDescriptor: String
    val jvmName: String
        get() = name

    fun array(dimensions: Int = 1): Type = ArrayType(this, dimensions)
}

sealed class PrimitiveType private constructor(
    val type: KClass<*>,
    override val size: Int,
    override val jvmName: String,
    override val jvmDescriptor: String,
) : Type {
    override val name: String = requireNotNull(type.qualifiedName)
    override val valueType: Type
        get() = this

    object VOID : PrimitiveType(Unit::class, 0, "java.lang.Void", "V")
    object BYTE : PrimitiveType(Byte::class, Byte.SIZE_BYTES, "java.lang.Byte", "B")
    object SHORT : PrimitiveType(Short::class, Short.SIZE_BYTES, "java.lang.Short", "S")
    object INT : PrimitiveType(Int::class, Int.SIZE_BYTES, "java.lang.Integer", "I")
    object LONG : PrimitiveType(Long::class, Long.SIZE_BYTES, "java.lang.Long", "J")
    object FLOAT : PrimitiveType(Float::class, Float.SIZE_BYTES, "java.lang.Float", "F")
    object DOUBLE : PrimitiveType(Double::class, Double.SIZE_BYTES, "java.lang.Double", "D")
    object BOOLEAN : PrimitiveType(Boolean::class, Byte.SIZE_BYTES, "java.lang.Boolean", "Z")

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = type.hashCode()
    override fun toString(): String = name
}

class ClassType(
    val descriptor: ClassDescriptor
) : Type {
    override val valueType: Type
        get() = this
    override val name: String by lazy { descriptor.toString() }
    override val size: Int = sizeOf<COpaquePointerVar>().toInt()
    override val jvmDescriptor: String = "L${descriptor.jvmName};"

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ClassType -> other.name == name
            else -> false
        }
    }

    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = name
}

class ArrayType(
    override val valueType: Type,
    val dimensions: Int = 1
) : Type {
    override val name: String = "Array<${valueType.name}>"
    override val jvmDescriptor: String = "[${valueType.jvmDescriptor}"
    override val size: Int = valueType.size * dimensions

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ArrayType -> valueType == other.valueType
                    && dimensions == other.dimensions

            else -> false
        }
    }

    override fun hashCode(): Int = 31 * valueType.hashCode() + dimensions
    override fun toString(): String = name
}