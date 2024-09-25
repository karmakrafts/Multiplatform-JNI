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

@ConsistentCopyVisibility
data class MethodDescriptor internal constructor(
    val name: String,
    val returnType: Type,
    val parameterTypes: List<Type>,
    val isStatic: Boolean,
) {
    companion object {
        fun create(closure: MethodDescriptorBuilder.() -> Unit): MethodDescriptor {
            val builder = MethodDescriptorBuilder()
            closure(builder)
            return builder.build()
        }
    }

    val jvmDescriptor: String by lazy {
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
    private var name: String = ""
    private var returnType: Type = PrimitiveType.VOID
    private val parameterTypes: ArrayList<Type> = ArrayList()
    private var isStatic: Boolean = false

    fun isStatic(isStatic: Boolean) {
        this.isStatic = isStatic
    }

    fun name(name: String) {
        this.name = name
    }

    fun returns(type: Type) {
        returnType = type
    }

    fun parameter(type: Type) {
        parameterTypes += type
    }

    internal fun build(): MethodDescriptor {
        require(name.isNotBlank())
        return MethodDescriptor(name, returnType, parameterTypes, isStatic)
    }
}