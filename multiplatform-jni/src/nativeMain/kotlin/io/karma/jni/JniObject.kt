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

import jni.JNIEnvVar
import jni.jobject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr

interface JvmObject {
    companion object {
        val NULL: JvmObjectRef = object : JvmObjectRef {
            override val handle: jobject? = null
            override fun delete(env: JNIEnvVar) {}
        }

        fun fromHandle(handle: jobject?): JvmObject {
            return if (handle == null) NULL
            else SimpleJvmObject(handle)
        }

        inline fun <reified T : JvmObject> JvmObject.cast(env: JNIEnvVar): T {
            return when (T::class) {
                JvmObject::class, JvmObjectRef::class -> this
                JvmString::class -> JvmString.fromUnchecked(cast(env, Type.STRING))
                JvmClass::class -> JvmClass.fromUnchecked(cast(env, Type.CLASS))
                else -> throw IllegalArgumentException("Unsupported type conversion $this -> ${T::class}")
            } as T
        }
    }

    val handle: jobject?

    fun isNull(): Boolean = handle == null

    fun createGlobalRef(env: JNIEnvVar): JvmObjectRef {
        return if (handle == null) NULL
        else JvmGlobalRef(env.pointed?.NewGlobalRef?.invoke(env.ptr, handle))
    }

    fun createLocalRef(env: JNIEnvVar): JvmObjectRef {
        return if (handle == null) NULL
        else JvmLocalRef(env.pointed?.NewLocalRef?.invoke(env.ptr, handle))
    }

    fun createWeakRef(env: JNIEnvVar): JvmObjectRef {
        return if (handle == null) NULL
        else JvmWeakRef(env.pointed?.NewWeakGlobalRef?.invoke(env.ptr, handle))
    }

    fun getTypeClass(env: JNIEnvVar): JvmClass =
        JvmClass.fromHandle(env.pointed?.GetObjectClass?.invoke(env.ptr, handle))

    fun cast(env: JNIEnvVar, type: Type): JvmObject {
        return JvmClass.find(env, type).let {
            it.findMethod(env) {
                name = "cast"
                returnType = Type.OBJECT
                parameterTypes += Type.OBJECT
            }.callObject(env, it) {
                put(this@JvmObject)
            }
        }
    }

    fun cast(env: JNIEnvVar, clazz: JvmClass): JvmObject = cast(env, clazz.getType(env))

    fun isInstance(env: JNIEnvVar, type: Type): Boolean {
        return JvmClass.find(env, type).let {
            it.findMethod(env) {
                name = "isInstance"
                returnType = PrimitiveType.BOOLEAN
                parameterTypes += Type.OBJECT
            }.callBoolean(env, it) {
                put(this@JvmObject)
            }
        }
    }

    fun isInstance(env: JNIEnvVar, clazz: JvmClass): Boolean = isInstance(env, clazz.getType(env))

    fun toKString(env: JNIEnvVar): String = jniScoped(env) {
        getTypeClass().findMethod {
            name = "toString"
            returnType = Type.STRING
        }.callObject(this@JvmObject)
            .cast<JvmString>()
            .value ?: "null"
    }
}

internal value class SimpleJvmObject(
    override val handle: jobject?
) : JvmObject

interface JvmObjectRef : JvmObject {
    fun delete(env: JNIEnvVar)
}

internal value class JvmGlobalRef(
    override val handle: jobject?
) : JvmObjectRef {
    override fun createGlobalRef(env: JNIEnvVar): JvmObjectRef = this
    override fun delete(env: JNIEnvVar) {
        env.pointed?.DeleteGlobalRef?.invoke(env.ptr, handle)
    }
}

internal value class JvmLocalRef(
    override val handle: jobject?
) : JvmObjectRef {
    override fun createLocalRef(env: JNIEnvVar): JvmObjectRef = this
    override fun delete(env: JNIEnvVar) {
        env.pointed?.DeleteLocalRef?.invoke(env.ptr, handle)
    }
}

internal value class JvmWeakRef(
    override val handle: jobject?
) : JvmObjectRef {
    override fun createWeakRef(env: JNIEnvVar): JvmObjectRef = this
    override fun delete(env: JNIEnvVar) {
        env.pointed?.DeleteWeakGlobalRef?.invoke(env.ptr, handle)
    }
}