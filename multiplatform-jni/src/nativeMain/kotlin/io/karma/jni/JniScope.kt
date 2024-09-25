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
import jni.jstring
import kotlinx.cinterop.ExperimentalForeignApi

value class JniScope(val env: JNIEnvVar) {
    fun jstring.toKString(): String = toKString(env)
    fun String.toJString(): jstring = toJString(env)

    // @formatter:off
    fun JvmClass.Companion.findOrNull(descriptor: ClassDescriptor): JvmClass? = findOrNull(env, descriptor)
    fun JvmClass.Companion.find(descriptor: ClassDescriptor): JvmClass = find(env, descriptor)

    fun JvmClass.findFieldOrNull(descriptor: FieldDescriptor): JvmField? = findFieldOrNull(env, descriptor)
    fun JvmClass.findFieldOrNull(closure: FieldDescriptorBuilder.() -> Unit): JvmField? = findFieldOrNull(env, closure)
    fun JvmClass.findField(descriptor: FieldDescriptor): JvmField = findField(env, descriptor)
    fun JvmClass.findField(closure: FieldDescriptorBuilder.() -> Unit): JvmField = findField(env, closure)

    fun JvmClass.findMethodOrNull(descriptor: MethodDescriptor): JvmMethod? = findMethodOrNull(env, descriptor)
    fun JvmClass.findMethodOrNull(closure: MethodDescriptorBuilder.() -> Unit): JvmMethod? = findMethodOrNull(env, closure)
    fun JvmClass.findMethod(descriptor: MethodDescriptor): JvmMethod = findMethod(env, descriptor)
    fun JvmClass.findMethod(closure: MethodDescriptorBuilder.() -> Unit): JvmMethod = findMethod(env, closure)

    fun JvmField.getByte(instance: jobject? = null): Byte = getByte(env, instance)
    fun JvmField.getShort(instance: jobject? = null): Short = getShort(env, instance)
    fun JvmField.getInt(instance: jobject? = null): Int = getInt(env, instance)
    fun JvmField.getLong(instance: jobject? = null): Long = getLong(env, instance)
    fun JvmField.getFloat(instance: jobject? = null): Float = getFloat(env, instance)
    fun JvmField.getDouble(instance: jobject? = null): Double = getDouble(env, instance)
    fun JvmField.getBoolean(instance: jobject? = null): Boolean = getBoolean(env, instance)

    fun JvmField.setByte(value: Byte, instance: jobject? = null) = setByte(env, value, instance)
    fun JvmField.setShort(value: Short, instance: jobject? = null) = setShort(env, value, instance)
    fun JvmField.setInt(value: Int, instance: jobject? = null) = setInt(env, value, instance)
    fun JvmField.setLong(value: Long, instance: jobject? = null) = setLong(env, value, instance)
    fun JvmField.setFloat(value: Float, instance: jobject? = null) = setFloat(env, value, instance)
    fun JvmField.setDouble(value: Double, instance: jobject? = null) = setDouble(env, value, instance)
    fun JvmField.setBoolean(value: Boolean, instance: jobject? = null) = setBoolean(env, value, instance)
    // @formatter:on

    fun <O : jobject> O.newGlobalRefOrNull(): O? = newGlobalRefOrNull(env)
    fun <O : jobject> O.newGlobalRef(): O = newGlobalRef(env)
    fun <O : jobject> O.deleteGlobalRef() = deleteGlobalRef(env)

    fun <O : jobject> O.newLocalRefOrNull(): O? = newLocalRefOrNull(env)
    fun <O : jobject> O.newLocalRef(): O = newLocalRef(env)
    fun <O : jobject> O.deleteLocalRef() = deleteLocalRef(env)

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