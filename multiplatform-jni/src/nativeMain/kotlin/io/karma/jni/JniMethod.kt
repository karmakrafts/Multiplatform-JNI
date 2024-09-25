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

import jni.jmethodID
import kotlinx.cinterop.ExperimentalForeignApi

interface MethodDescriptor {
    val name: String
    val returnType: Type
    val parameterTypes: List<Type>
    val isStatic: Boolean
    val jvmDescriptor: String

    companion object {
        fun create(closure: MethodDescriptorBuilder.() -> Unit): MethodDescriptor {
            return MethodDescriptorBuilder().apply(closure).build()
        }
    }
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

class JvmMethod(
    val enclosingClass: JvmClass,
    val descriptor: MethodDescriptor,
    val handle: jmethodID,
) : MethodDescriptor by descriptor