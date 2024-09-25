package io.karma.jni

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