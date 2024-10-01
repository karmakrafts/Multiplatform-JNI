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
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr

interface JvmObject {
    companion object {
        val NULL: JvmObjectRef = object : JvmObjectRef {
            @property:OptIn(UnsafeJniApi::class)
            override val handle: JvmObjectHandle? = null

            @UnsafeJniApi
            override fun delete(env: JniEnvironment) {
            }
        }

        @UnsafeJniApi
        fun fromHandle(handle: JvmObjectHandle?): JvmObject {
            return if (handle == null) NULL
            else SimpleJvmObject(handle)
        }

        @UnsafeJniApi
        inline fun <reified T : JvmObject> JvmObject.uncheckedCast(): T {
            return when (T::class) {
                JvmObject::class, JvmObjectRef::class -> this
                JvmString::class -> JvmString.fromUnchecked(this)
                JvmClass::class -> JvmClass.fromUnchecked(this)
                JvmArray::class, JvmGenericArray::class -> JvmGenericArray.fromUnchecked(this)
                JvmByteArray::class -> JvmByteArray.fromUnchecked(this)
                JvmShortArray::class -> JvmShortArray.fromUnchecked(this)
                JvmIntArray::class -> JvmIntArray.fromUnchecked(this)
                JvmLongArray::class -> JvmLongArray.fromUnchecked(this)
                JvmFloatArray::class -> JvmFloatArray.fromUnchecked(this)
                JvmDoubleArray::class -> JvmDoubleArray.fromUnchecked(this)
                JvmBooleanArray::class -> JvmBooleanArray.fromUnchecked(this)
                JvmCharArray::class -> JvmCharArray.fromUnchecked(this)
                JvmObjectArray::class -> JvmObjectArray.fromUnchecked(this)
                else -> throw IllegalArgumentException("Unsupported type conversion $this -> ${T::class}")
            } as T
        }
    }

    @property:UnsafeJniApi
    val handle: JvmObjectHandle?

    @OptIn(UnsafeJniApi::class)
    fun isNull(): Boolean = handle == null

    @UnsafeJniApi
    fun createGlobalRef(env: JniEnvironment): JvmObjectRef {
        return if (handle == null) NULL
        else JvmGlobalRef(env.pointed?.NewGlobalRef?.invoke(env.ptr, handle))
    }

    @UnsafeJniApi
    fun createLocalRef(env: JniEnvironment): JvmObjectRef {
        return if (handle == null) NULL
        else JvmLocalRef(env.pointed?.NewLocalRef?.invoke(env.ptr, handle))
    }

    @UnsafeJniApi
    fun createWeakRef(env: JniEnvironment): JvmObjectRef {
        return if (handle == null) NULL
        else JvmWeakRef(env.pointed?.NewWeakGlobalRef?.invoke(env.ptr, handle))
    }

    @OptIn(UnsafeJniApi::class)
    fun getTypeClass(env: JniEnvironment): JvmClass =
        JvmClass.fromHandle(env.pointed?.GetObjectClass?.invoke(env.ptr, handle))

    fun cast(env: JniEnvironment, type: Type): JvmObject = jniScoped(env) {
        return JvmClass.find(type).let {
            it.findMethod {
                name = "cast"
                returnType = Type.OBJECT
                parameterTypes += Type.OBJECT
                callType = CallType.DIRECT
            }.callObject(it) {
                put(this@JvmObject)
            }
        }
    }

    fun cast(env: JniEnvironment, clazz: JvmClass): JvmObject = cast(env, clazz.getType(env))

    fun isInstance(env: JniEnvironment, type: Type): Boolean = jniScoped(env) {
        return JvmClass.find(type).let {
            it.findMethod {
                name = "isInstance"
                returnType = PrimitiveType.BOOLEAN
                parameterTypes += Type.OBJECT
                callType = CallType.DIRECT
            }.callBoolean(it) {
                put(this@JvmObject)
            }
        }
    }

    fun isInstance(env: JniEnvironment, clazz: JvmClass): Boolean =
        isInstance(env, clazz.getType(env))

    fun toKString(env: JniEnvironment): String = jniScoped(env) {
        typeClass.findMethod {
            name = "toString"
            returnType = Type.STRING
        }.callObject<JvmString>(this@JvmObject)
            .value ?: "null"
    }
}

internal value class SimpleJvmObject @UnsafeJniApi internal constructor(
    @property:UnsafeJniApi override val handle: JvmObjectHandle?
) : JvmObject

interface JvmObjectRef : JvmObject {
    @UnsafeJniApi
    fun delete(env: JniEnvironment)
}

internal value class JvmGlobalRef @UnsafeJniApi internal constructor(
    @property:UnsafeJniApi override val handle: JvmObjectHandle?
) : JvmObjectRef {
    @UnsafeJniApi
    override fun createGlobalRef(env: JniEnvironment): JvmObjectRef = this

    @UnsafeJniApi
    override fun delete(env: JniEnvironment) {
        env.pointed?.DeleteGlobalRef?.invoke(env.ptr, handle)
    }
}

internal value class JvmLocalRef @UnsafeJniApi internal constructor(
    @property:UnsafeJniApi override val handle: JvmObjectHandle?
) : JvmObjectRef {
    @UnsafeJniApi
    override fun createLocalRef(env: JniEnvironment): JvmObjectRef = this

    @UnsafeJniApi
    override fun delete(env: JniEnvironment) {
        env.pointed?.DeleteLocalRef?.invoke(env.ptr, handle)
    }
}

internal value class JvmWeakRef @UnsafeJniApi internal constructor(
    @property:UnsafeJniApi override val handle: JvmObjectHandle?
) : JvmObjectRef {
    @UnsafeJniApi
    override fun createWeakRef(env: JniEnvironment): JvmObjectRef = this

    @UnsafeJniApi
    override fun delete(env: JniEnvironment) {
        env.pointed?.DeleteWeakGlobalRef?.invoke(env.ptr, handle)
    }
}