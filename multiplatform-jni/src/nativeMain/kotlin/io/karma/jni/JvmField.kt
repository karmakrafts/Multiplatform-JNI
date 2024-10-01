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

import io.karma.jni.JvmObject.Companion.uncheckedCast
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlin.experimental.ExperimentalNativeApi

interface FieldDescriptor {
    val name: String
    val type: Type
    val isStatic: Boolean
    val jvmDescriptor: String

    companion object {
        fun create(closure: FieldDescriptorBuilder.() -> Unit): FieldDescriptor {
            return FieldDescriptorBuilder().apply(closure).build()
        }
    }
}

class FieldDescriptorBuilder internal constructor() {
    var name: String = ""
    var type: Type? = null
    var isStatic: Boolean = false

    fun setFrom(descriptor: FieldDescriptor) {
        name = descriptor.name
        type = descriptor.type
        isStatic = descriptor.isStatic
    }

    internal fun build(): SimpleFieldDescriptor {
        require(name.isNotBlank()) { "Field name must be specified" }
        require(type != null) { "Field type must be specified" }
        return SimpleFieldDescriptor(name, type!!, isStatic)
    }
}

internal data class SimpleFieldDescriptor(
    override val name: String,
    override val type: Type,
    override val isStatic: Boolean,
) : FieldDescriptor {
    override val jvmDescriptor: String = type.jvmDescriptor

    override fun toString(): String = "$name:$jvmDescriptor"
}

class JvmField(
    val enclosingClass: JvmClass,
    val descriptor: FieldDescriptor,
    @property:UnsafeJniApi val id: JvmFieldId,
) : FieldDescriptor by descriptor, VisibilityProvider, AnnotationProvider {
    // Getters

    @OptIn(UnsafeJniApi::class)
    fun getByte(env: JniEnvironment, instance: JvmObject = JvmObject.NULL): Byte {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticByteField?.invoke(env.ptr, enclosingClass.handle, id)
                ?: 0
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetByteField?.invoke(env.ptr, instance.handle, id) ?: 0
    }

    @OptIn(UnsafeJniApi::class)
    fun getShort(env: JniEnvironment, instance: JvmObject = JvmObject.NULL): Short {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticShortField?.invoke(env.ptr, enclosingClass.handle, id)
                ?: 0
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetShortField?.invoke(env.ptr, instance.handle, id) ?: 0
    }

    @OptIn(UnsafeJniApi::class)
    fun getInt(env: JniEnvironment, instance: JvmObject = JvmObject.NULL): Int {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticIntField?.invoke(env.ptr, enclosingClass.handle, id)
                ?: 0
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetIntField?.invoke(env.ptr, instance.handle, id) ?: 0
    }

    @OptIn(UnsafeJniApi::class)
    fun getLong(env: JniEnvironment, instance: JvmObject = JvmObject.NULL): Long {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticLongField?.invoke(env.ptr, enclosingClass.handle, id)
                ?: 0
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetLongField?.invoke(env.ptr, instance.handle, id) ?: 0
    }

    @OptIn(UnsafeJniApi::class)
    fun getFloat(env: JniEnvironment, instance: JvmObject = JvmObject.NULL): Float {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticFloatField?.invoke(env.ptr, enclosingClass.handle, id)
                ?: 0F
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetFloatField?.invoke(env.ptr, instance.handle, id) ?: 0F
    }

    @OptIn(UnsafeJniApi::class)
    fun getDouble(env: JniEnvironment, instance: JvmObject = JvmObject.NULL): Double {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticDoubleField?.invoke(env.ptr, enclosingClass.handle, id)
                ?: 0.0
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetDoubleField?.invoke(env.ptr, instance.handle, id) ?: 0.0
    }

    @OptIn(UnsafeJniApi::class)
    fun getBoolean(env: JniEnvironment, instance: JvmObject = JvmObject.NULL): Boolean {
        if (descriptor.isStatic) {
            return env.pointed?.GetStaticBooleanField?.invoke(
                env.ptr,
                enclosingClass.handle,
                id
            )?.toKBoolean() ?: false
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        return env.pointed?.GetBooleanField?.invoke(env.ptr, instance.handle, id)?.toKBoolean()
            ?: false
    }

    @OptIn(ExperimentalNativeApi::class, UnsafeJniApi::class)
    fun getChar(env: JniEnvironment, instance: JvmObject = JvmObject.NULL): Char {
        if (descriptor.isStatic) {
            return Char.toChars(
                env.pointed?.GetStaticCharField?.invoke(env.ptr, enclosingClass.handle, id)?.toInt()
                    ?: 0
            )[0]
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        return Char.toChars(
            env.pointed?.GetCharField?.invoke(env.ptr, instance.handle, id)?.toInt() ?: 0
        )[0]
    }

    @OptIn(UnsafeJniApi::class)
    fun getObject(env: JniEnvironment, instance: JvmObject = JvmObject.NULL): JvmObject {
        if (descriptor.isStatic) {
            return JvmObject.fromHandle(
                env.pointed?.GetStaticObjectField?.invoke(
                    env.ptr,
                    enclosingClass.handle,
                    id
                )
            )
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        return JvmObject.fromHandle(
            env.pointed?.GetObjectField?.invoke(
                env.ptr,
                instance.handle,
                id
            )
        )
    }

    @OptIn(UnsafeJniApi::class)
    inline fun <reified R : JvmObject> getObject(
        env: JniEnvironment,
        instance: JvmObject = JvmObject.NULL
    ): R {
        return getObject(env, instance).uncheckedCast<R>()
    }

    @OptIn(UnsafeJniApi::class)
    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline fun <reified R> get(env: JniEnvironment, instance: JvmObject = JvmObject.NULL): R {
        return when (R::class) {
            Byte::class -> getByte(env, instance)
            Short::class -> getShort(env, instance)
            Int::class -> getInt(env, instance)
            Long::class -> getLong(env, instance)
            Float::class -> getFloat(env, instance)
            Double::class -> getDouble(env, instance)
            Boolean::class -> getBoolean(env, instance)
            Char::class -> getChar(env, instance)
            JvmObject::class -> getObject(env, instance)
            JvmString::class -> JvmString.fromUnchecked(getObject(env, instance))
            JvmClass::class -> JvmClass.fromUnchecked(getObject(env, instance))
            JvmArray::class, JvmGenericArray::class -> JvmGenericArray.fromUnchecked(
                getObject(
                    env,
                    instance
                )
            )

            JvmByteArray::class -> JvmByteArray.fromUnchecked(getObject(env, instance))
            JvmShortArray::class -> JvmShortArray.fromUnchecked(getObject(env, instance))
            JvmIntArray::class -> JvmIntArray.fromUnchecked(getObject(env, instance))
            JvmLongArray::class -> JvmLongArray.fromUnchecked(getObject(env, instance))
            JvmFloatArray::class -> JvmFloatArray.fromUnchecked(getObject(env, instance))
            JvmDoubleArray::class -> JvmDoubleArray.fromUnchecked(getObject(env, instance))
            JvmBooleanArray::class -> JvmBooleanArray.fromUnchecked(getObject(env, instance))
            JvmCharArray::class -> JvmCharArray.fromUnchecked(getObject(env, instance))
            JvmObjectArray::class -> JvmObjectArray.fromUnchecked(getObject(env, instance))
            else -> throw IllegalArgumentException("Unsupported field type")
        } as R
    }

    // Setters

    @OptIn(UnsafeJniApi::class)
    fun setByte(env: JniEnvironment, value: Byte, instance: JvmObject = JvmObject.NULL) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticByteField?.invoke(env.ptr, enclosingClass.handle, id, value)
            return
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        env.pointed?.SetByteField?.invoke(env.ptr, instance.handle, id, value.convert())
    }

    @OptIn(UnsafeJniApi::class)
    fun setShort(env: JniEnvironment, value: Short, instance: JvmObject = JvmObject.NULL) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticShortField?.invoke(env.ptr, enclosingClass.handle, id, value)
            return
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        env.pointed?.SetShortField?.invoke(env.ptr, instance.handle, id, value.convert())
    }

    @OptIn(UnsafeJniApi::class)
    fun setInt(env: JniEnvironment, value: Int, instance: JvmObject = JvmObject.NULL) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticIntField?.invoke(env.ptr, enclosingClass.handle, id, value)
            return
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        env.pointed?.SetIntField?.invoke(env.ptr, instance.handle, id, value.convert())
    }

    @OptIn(UnsafeJniApi::class)
    fun setLong(env: JniEnvironment, value: Long, instance: JvmObject = JvmObject.NULL) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticLongField?.invoke(env.ptr, enclosingClass.handle, id, value)
            return
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        env.pointed?.SetLongField?.invoke(env.ptr, instance.handle, id, value.convert())
    }

    @OptIn(UnsafeJniApi::class)
    fun setFloat(env: JniEnvironment, value: Float, instance: JvmObject = JvmObject.NULL) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticFloatField?.invoke(env.ptr, enclosingClass.handle, id, value)
            return
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        env.pointed?.SetFloatField?.invoke(env.ptr, instance.handle, id, value)
    }

    @OptIn(UnsafeJniApi::class)
    fun setDouble(env: JniEnvironment, value: Double, instance: JvmObject = JvmObject.NULL) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticDoubleField?.invoke(env.ptr, enclosingClass.handle, id, value)
            return
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        env.pointed?.SetDoubleField?.invoke(env.ptr, instance.handle, id, value)
    }

    @OptIn(UnsafeJniApi::class)
    fun setBoolean(env: JniEnvironment, value: Boolean, instance: JvmObject = JvmObject.NULL) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticBooleanField?.invoke(
                env.ptr,
                enclosingClass.handle,
                id,
                value.toJBoolean()
            )
            return
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        env.pointed?.SetBooleanField?.invoke(env.ptr, instance.handle, id, value.toJBoolean())
    }

    @OptIn(UnsafeJniApi::class)
    fun setChar(env: JniEnvironment, value: Char, instance: JvmObject = JvmObject.NULL) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticCharField?.invoke(
                env.ptr,
                enclosingClass.handle,
                id,
                value.code.toUShort()
            )
            return
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        env.pointed?.SetCharField?.invoke(env.ptr, instance.handle, id, value.code.toUShort())
    }

    @OptIn(UnsafeJniApi::class)
    fun setObject(env: JniEnvironment, value: JvmObject, instance: JvmObject = JvmObject.NULL) {
        if (descriptor.isStatic) {
            env.pointed?.SetStaticObjectField?.invoke(
                env.ptr,
                enclosingClass.handle,
                id,
                value.handle
            )
            return
        }
        require(!instance.isNull()) { "Instance must not be null for non-static fields" }
        env.pointed?.SetObjectField?.invoke(env.ptr, instance.handle, id, value.handle)
    }

    inline fun <reified R> set(
        env: JniEnvironment,
        value: R,
        instance: JvmObject = JvmObject.NULL
    ) {
        when (R::class) {
            Byte::class -> setByte(env, value as Byte, instance)
            Short::class -> setShort(env, value as Short, instance)
            Int::class -> setInt(env, value as Int, instance)
            Long::class -> setLong(env, value as Long, instance)
            Float::class -> setFloat(env, value as Float, instance)
            Double::class -> setDouble(env, value as Double, instance)
            Boolean::class -> setBoolean(env, value as Boolean, instance)
            Char::class -> setChar(env, value as Char, instance)
            JvmObject::class, JvmString::class, JvmClass::class, JvmArray::class,
            JvmGenericArray::class, JvmByteArray::class, JvmShortArray::class, JvmIntArray::class,
            JvmLongArray::class, JvmFloatArray::class, JvmDoubleArray::class, JvmBooleanArray::class,
            JvmCharArray::class, JvmObjectArray::class -> setObject(
                env,
                value as JvmObject,
                instance
            )

            else -> throw IllegalArgumentException("Unsupported field type")
        }
    }

    @OptIn(UnsafeJniApi::class)
    fun getInstance(env: JniEnvironment): JvmObject =
        JvmObject.fromHandle(
            env.pointed?.ToReflectedField?.invoke(
                env.ptr,
                enclosingClass.handle,
                id,
                descriptor.isStatic.toJBoolean()
            )
        )

    override fun getVisibility(env: JniEnvironment): JvmVisibility = jniScoped(env) {
        JvmClass.find(Type.FIELD).findMethod {
            name = "getModifiers"
            returnType = PrimitiveType.INT
            callType = CallType.DIRECT
        }.callInt(instance).toUShort().let { modifiers ->
            JvmVisibility.entries.find {
                modifiers and it.jvmValue == it.jvmValue
            } ?: JvmVisibility.PRIVATE
        }
    }

    override fun hasAnnotation(env: JniEnvironment, type: Type): Boolean = jniScoped(env) {
        JvmClass.find(Type.FIELD).findMethod {
            name = "isAnnotationPresent"
            returnType = PrimitiveType.BOOLEAN
            parameterTypes += Type.CLASS
            callType = CallType.DIRECT
        }.callBoolean(instance) {
            put(JvmClass.find(type))
        }
    }

    override fun getAnnotation(env: JniEnvironment, type: Type): JvmObject = jniScoped(env) {
        JvmClass.find(Type.FIELD).findMethod {
            name = "getAnnotation"
            returnType = type
            parameterTypes += Type.CLASS
            callType = CallType.DIRECT
        }.callObject(instance) {
            put(JvmClass.find(type))
        }
    }
}