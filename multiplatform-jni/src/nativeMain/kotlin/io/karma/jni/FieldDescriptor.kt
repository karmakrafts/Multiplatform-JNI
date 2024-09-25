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
data class FieldDescriptor internal constructor(
    val name: String,
    val type: Type,
    val isStatic: Boolean,
) {
    companion object {
        fun create(closure: FieldDescriptorBuilder.() -> Unit): FieldDescriptor {
            val builder = FieldDescriptorBuilder()
            closure(builder)
            return builder.build()
        }
    }

    val jvmDescriptor: String = type.jvmDescriptor
}

class FieldDescriptorBuilder internal constructor() {
    private var name: String = ""
    private var type: Type? = null
    private var isStatic: Boolean = false

    fun name(name: String) {
        this.name = name
    }

    fun type(type: Type) {
        this.type = type
    }

    fun isStatic(isStatic: Boolean) {
        this.isStatic = isStatic
    }

    internal fun build(): FieldDescriptor {
        require(name.isNotBlank()) { "Field name must be specified" }
        require(type != null) { "Field type must be specified" }
        return FieldDescriptor(name, type!!, isStatic)
    }
}