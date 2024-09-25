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

package io.karma.jni

import kotlin.reflect.KClass

interface Type {
    companion object {
        fun of(qualifiedName: String): Type = ClassType(qualifiedName)
        fun of(type: KClass<*>): Type = ClassType(requireNotNull(type.qualifiedName))
        inline fun <reified T> of(): Type = of(T::class)
    }

    val name: String
    val valueType: Type

    val jvmDescriptor: String
    val jvmName: String
        get() = name

    fun array(dimensions: Int = 1): Type = ArrayType(this, dimensions)
}

sealed class PrimitiveType private constructor(
    val type: KClass<*>,
    override val jvmName: String,
    override val jvmDescriptor: String,
) : Type {
    override val name: String = requireNotNull(type.qualifiedName)
    override val valueType: Type
        get() = this

    object VOID : PrimitiveType(Unit::class, "java.lang.Void", "V")
    object BYTE : PrimitiveType(Byte::class, "java.lang.Byte", "B")
    object SHORT : PrimitiveType(Short::class, "java.lang.Short", "S")
    object INT : PrimitiveType(Int::class, "java.lang.Integer", "I")
    object LONG : PrimitiveType(Long::class, "java.lang.Long", "J")
    object FLOAT : PrimitiveType(Float::class, "java.lang.Float", "F")
    object DOUBLE : PrimitiveType(Double::class, "java.lang.Double", "D")
    object BOOLEAN : PrimitiveType(Boolean::class, "java.lang.Boolean", "Z")

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = type.hashCode()
    override fun toString(): String = name
}

class ClassType(
    override val name: String
) : Type {
    override val valueType: Type
        get() = this
    override val jvmDescriptor: String = "L$name;"

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