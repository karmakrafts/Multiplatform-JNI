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
            override val handle: JvmObjectHandle? = null
            override fun delete(env: JniEnvironment) {}
        }

        fun fromHandle(handle: JvmObjectHandle?): JvmObject {
            return if (handle == null) NULL
            else SimpleJvmObject(handle)
        }

        @OptIn(UnsafeJniApi::class)
        inline fun <reified T : JvmObject> JvmObject.cast(env: JniEnvironment): T {
            return when (T::class) {
                JvmObject::class, JvmObjectRef::class -> this
                JvmString::class -> JvmString.fromUnchecked(cast(env, Type.STRING))
                JvmClass::class -> JvmClass.fromUnchecked(cast(env, Type.CLASS))
                JvmArray::class -> JvmArray.fromUnchecked(this) // We can't do any checked casts here..
                else -> throw IllegalArgumentException("Unsupported type conversion $this -> ${T::class}")
            } as T
        }
    }

    val handle: JvmObjectHandle?

    fun isNull(): Boolean = handle == null

    fun createGlobalRef(env: JniEnvironment): JvmObjectRef {
        return if (handle == null) NULL
        else JvmGlobalRef(env.pointed?.NewGlobalRef?.invoke(env.ptr, handle))
    }

    fun createLocalRef(env: JniEnvironment): JvmObjectRef {
        return if (handle == null) NULL
        else JvmLocalRef(env.pointed?.NewLocalRef?.invoke(env.ptr, handle))
    }

    fun createWeakRef(env: JniEnvironment): JvmObjectRef {
        return if (handle == null) NULL
        else JvmWeakRef(env.pointed?.NewWeakGlobalRef?.invoke(env.ptr, handle))
    }

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
        }.callObject(this@JvmObject)
            .cast<JvmString>()
            .value ?: "null"
    }
}

internal value class SimpleJvmObject(
    override val handle: JvmObjectHandle?
) : JvmObject

interface JvmObjectRef : JvmObject {
    fun delete(env: JniEnvironment)
}

internal value class JvmGlobalRef(
    override val handle: JvmObjectHandle?
) : JvmObjectRef {
    override fun createGlobalRef(env: JniEnvironment): JvmObjectRef = this
    override fun delete(env: JniEnvironment) {
        env.pointed?.DeleteGlobalRef?.invoke(env.ptr, handle)
    }
}

internal value class JvmLocalRef(
    override val handle: JvmObjectHandle?
) : JvmObjectRef {
    override fun createLocalRef(env: JniEnvironment): JvmObjectRef = this
    override fun delete(env: JniEnvironment) {
        env.pointed?.DeleteLocalRef?.invoke(env.ptr, handle)
    }
}

internal value class JvmWeakRef(
    override val handle: JvmObjectHandle?
) : JvmObjectRef {
    override fun createWeakRef(env: JniEnvironment): JvmObjectRef = this
    override fun delete(env: JniEnvironment) {
        env.pointed?.DeleteWeakGlobalRef?.invoke(env.ptr, handle)
    }
}