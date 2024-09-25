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
import jni.jstring
import kotlinx.cinterop.ExperimentalForeignApi

value class JniScope(val env: JNIEnvVar) {
    fun jstring.toKString(): String = toKString(env)
    fun String.toJString(): jstring = toJString(env)

    fun JvmClass.Companion.findOrNull(descriptor: ClassDescriptor): JvmClass? =
        findOrNull(env, descriptor)

    fun JvmClass.Companion.find(descriptor: ClassDescriptor): JvmClass = find(env, descriptor)

    fun JvmClass.findFieldOrNull(descriptor: FieldDescriptor): JvmField? =
        findFieldOrNull(env, descriptor)

    fun JvmClass.findFieldOrNull(closure: FieldDescriptorBuilder.() -> Unit): JvmField? =
        findFieldOrNull(env, closure)

    fun JvmClass.findField(descriptor: FieldDescriptor): JvmField = findField(env, descriptor)
    fun JvmClass.findField(closure: FieldDescriptorBuilder.() -> Unit): JvmField =
        findField(env, closure)

    fun JvmClass.findMethodOrNull(descriptor: MethodDescriptor): JvmMethod? =
        findMethodOrNull(env, descriptor)

    fun JvmClass.findMethodOrNull(closure: MethodDescriptorBuilder.() -> Unit): JvmMethod? =
        findMethodOrNull(env, closure)

    fun JvmClass.findMethod(descriptor: MethodDescriptor): JvmMethod = findMethod(env, descriptor)
    fun JvmClass.findMethod(closure: MethodDescriptorBuilder.() -> Unit): JvmMethod =
        findMethod(env, closure)

    fun JvmField.getByte(instance: JvmObject = JvmNull): Byte = getByte(env, instance)
    fun JvmField.getShort(instance: JvmObject = JvmNull): Short = getShort(env, instance)
    fun JvmField.getInt(instance: JvmObject = JvmNull): Int = getInt(env, instance)
    fun JvmField.getLong(instance: JvmObject = JvmNull): Long = getLong(env, instance)
    fun JvmField.getFloat(instance: JvmObject = JvmNull): Float = getFloat(env, instance)
    fun JvmField.getDouble(instance: JvmObject = JvmNull): Double = getDouble(env, instance)
    fun JvmField.getBoolean(instance: JvmObject = JvmNull): Boolean = getBoolean(env, instance)
    fun JvmField.getObject(instance: JvmObject = JvmNull): JvmObject = getObject(env, instance)
    inline fun <reified R> JvmField.get(instance: JvmObject = JvmNull): R = get<R>(env, instance)

    fun JvmField.setByte(value: Byte, instance: JvmObject = JvmNull) = setByte(env, value, instance)
    fun JvmField.setShort(value: Short, instance: JvmObject = JvmNull) =
        setShort(env, value, instance)

    fun JvmField.setInt(value: Int, instance: JvmObject = JvmNull) = setInt(env, value, instance)
    fun JvmField.setLong(value: Long, instance: JvmObject = JvmNull) = setLong(env, value, instance)
    fun JvmField.setFloat(value: Float, instance: JvmObject = JvmNull) =
        setFloat(env, value, instance)

    fun JvmField.setDouble(value: Double, instance: JvmObject = JvmNull) =
        setDouble(env, value, instance)

    fun JvmField.setBoolean(value: Boolean, instance: JvmObject = JvmNull) =
        setBoolean(env, value, instance)

    fun JvmField.setObject(value: JvmObject, instance: JvmObject = JvmNull) =
        setObject(env, value, instance)

    inline fun <reified R> JvmField.set(value: R, instance: JvmObject = JvmNull) =
        set<R>(env, value, instance)

    fun JvmMethod.callByte(instance: JvmObject = JvmNull, closure: ArgumentScope.() -> Unit): Byte =
        callByte(env, instance, closure)

    fun JvmMethod.callShort(
        instance: JvmObject = JvmNull,
        closure: ArgumentScope.() -> Unit
    ): Short =
        callShort(env, instance, closure)

    fun JvmMethod.callInt(instance: JvmObject = JvmNull, closure: ArgumentScope.() -> Unit): Int =
        callInt(env, instance, closure)

    fun JvmMethod.callLong(instance: JvmObject = JvmNull, closure: ArgumentScope.() -> Unit): Long =
        callLong(env, instance, closure)

    fun JvmMethod.callFloat(
        instance: JvmObject = JvmNull,
        closure: ArgumentScope.() -> Unit
    ): Float =
        callFloat(env, instance, closure)

    fun JvmMethod.callDouble(
        instance: JvmObject = JvmNull,
        closure: ArgumentScope.() -> Unit
    ): Double =
        callDouble(env, instance, closure)

    fun JvmMethod.callBoolean(
        instance: JvmObject = JvmNull,
        closure: ArgumentScope.() -> Unit
    ): Boolean = callBoolean(env, instance, closure)

    fun JvmMethod.callObject(
        instance: JvmObject = JvmNull,
        closure: ArgumentScope.() -> Unit
    ): JvmObject = callObject(env, instance, closure)

    inline fun <reified R> JvmMethod.call(
        instance: JvmObject = JvmNull,
        closure: ArgumentScope.() -> Unit
    ): R = call<R>(env, instance, closure)

    fun JvmObject.createGlobalRef(): JvmObjectRef = createGlobalRef(env)
    fun JvmObject.createLocalRef(): JvmObjectRef = createLocalRef(env)
    fun JvmObject.createWeakRef(): JvmObjectRef = createWeakRef(env)
    fun JvmObjectRef.delete() = delete(env)

    inline fun <reified R> whileDetached(scope: () -> R): R {
        JniPlatform.detach()
        val result = scope()
        JniPlatform.attach()
        return result
    }
}

inline fun <reified R> jniScoped(scope: JniScope.() -> R): R {
    val result =
        scope(JniScope(requireNotNull(JniPlatform.attach()) { "Could not access JNI environment" }))
    JniPlatform.detach()
    return result
}