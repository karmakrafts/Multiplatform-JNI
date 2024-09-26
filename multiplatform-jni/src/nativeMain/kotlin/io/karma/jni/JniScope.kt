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

import io.karma.jni.JvmObject.Companion.cast
import jni.JNIEnvVar
import jni.jstring
import kotlinx.cinterop.ExperimentalForeignApi

value class JniScope(val env: JNIEnvVar) {
    fun jstring.toKString(): String = toKString(env)
    fun String.toJString(): jstring = toJString(env)

    // @formatter:off
    fun JvmClass.Companion.findOrNull(type: Type): JvmClass? =
        findOrNull(env, type)
    fun JvmClass.Companion.find(type: Type): JvmClass = find(env, type)

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

    fun JvmClass.hasAnnotation(type: Type): Boolean = hasAnnotation(env, type)
    fun JvmClass.getAnnotation(type: Type): JvmObject = getAnnotation(env, type)

    val JvmClass.type: Type
        get() = getType(env)
    val JvmClass.visibility: JvmVisibility
        get() = getVisibility(env)

    fun JvmField.getByte(instance: JvmObject = JvmObject.NULL): Byte = getByte(env, instance)
    fun JvmField.getShort(instance: JvmObject = JvmObject.NULL): Short = getShort(env, instance)
    fun JvmField.getInt(instance: JvmObject = JvmObject.NULL): Int = getInt(env, instance)
    fun JvmField.getLong(instance: JvmObject = JvmObject.NULL): Long = getLong(env, instance)
    fun JvmField.getFloat(instance: JvmObject = JvmObject.NULL): Float = getFloat(env, instance)
    fun JvmField.getDouble(instance: JvmObject = JvmObject.NULL): Double = getDouble(env, instance)
    fun JvmField.getBoolean(instance: JvmObject = JvmObject.NULL): Boolean = getBoolean(env, instance)
    fun JvmField.getObject(instance: JvmObject = JvmObject.NULL): JvmObject = getObject(env, instance)
    inline fun <reified R> JvmField.get(instance: JvmObject = JvmObject.NULL): R = get<R>(env, instance)

    fun JvmField.setByte(value: Byte, instance: JvmObject = JvmObject.NULL) = setByte(env, value, instance)
    fun JvmField.setShort(value: Short, instance: JvmObject = JvmObject.NULL) = setShort(env, value, instance)
    fun JvmField.setInt(value: Int, instance: JvmObject = JvmObject.NULL) = setInt(env, value, instance)
    fun JvmField.setLong(value: Long, instance: JvmObject = JvmObject.NULL) = setLong(env, value, instance)
    fun JvmField.setFloat(value: Float, instance: JvmObject = JvmObject.NULL) = setFloat(env, value, instance)
    fun JvmField.setDouble(value: Double, instance: JvmObject = JvmObject.NULL) = setDouble(env, value, instance)
    fun JvmField.setBoolean(value: Boolean, instance: JvmObject = JvmObject.NULL) = setBoolean(env, value, instance)
    fun JvmField.setObject(value: JvmObject, instance: JvmObject = JvmObject.NULL) = setObject(env, value, instance)
    inline fun <reified R> JvmField.set(value: R, instance: JvmObject = JvmObject.NULL) = set<R>(env, value, instance)
    // @formatter:on

    val JvmField.instance: JvmObject
        get() = getInstance(env)
    val JvmField.visibility: JvmVisibility
        get() = getVisibility(env)

    fun JvmMethod.callByte(
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): Byte = callByte(env, instance, closure)

    fun JvmMethod.callShort(
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): Short = callShort(env, instance, closure)

    fun JvmMethod.callInt(
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): Int = callInt(env, instance, closure)

    fun JvmMethod.callLong(
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): Long = callLong(env, instance, closure)

    fun JvmMethod.callFloat(
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): Float = callFloat(env, instance, closure)

    fun JvmMethod.callDouble(
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): Double = callDouble(env, instance, closure)

    fun JvmMethod.callBoolean(
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): Boolean = callBoolean(env, instance, closure)

    fun JvmMethod.callObject(
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): JvmObject = callObject(env, instance, closure)

    inline fun <reified R> JvmMethod.call(
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): R = call<R>(env, instance, closure)

    val JvmMethod.instance: JvmObject
        get() = getInstance(env)
    val JvmMethod.visibility: JvmVisibility
        get() = getVisibility(env)

    fun JvmObject.createGlobalRef(): JvmObjectRef = createGlobalRef(env)
    fun JvmObject.createLocalRef(): JvmObjectRef = createLocalRef(env)
    fun JvmObject.createWeakRef(): JvmObjectRef = createWeakRef(env)
    fun JvmObject.getTypeClass(): JvmClass = getTypeClass(env)
    fun JvmObject.cast(type: Type): JvmObject = cast(env, type)
    fun JvmObject.cast(clazz: JvmClass): JvmObject = cast(env, clazz)
    fun JvmObject.isInstance(type: Type): Boolean = isInstance(env, type)
    fun JvmObject.isInstance(clazz: JvmClass): Boolean = isInstance(env, clazz)
    fun JvmObjectRef.delete() = delete(env)

    inline fun <reified T : JvmObject> JvmObject.cast(): T = cast<T>(env)

    fun JvmString.Companion.of(value: String): JvmString = of(env, value)

    val JvmString.value: String?
        get() = get(env)
    val JvmString.length: Int
        get() = getLength(env)
}

inline fun <reified R> jniScoped(scope: JniScope.() -> R): R {
    val result =
        scope(JniScope(requireNotNull(JniPlatform.attach()) { "Could not access JNI environment" }))
    JniPlatform.detach()
    return result
}

inline fun <reified R> jniScoped(env: JNIEnvVar, scope: JniScope.() -> R): R {
    return scope(JniScope(env))
}