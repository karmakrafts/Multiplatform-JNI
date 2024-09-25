package io.karma.jni

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