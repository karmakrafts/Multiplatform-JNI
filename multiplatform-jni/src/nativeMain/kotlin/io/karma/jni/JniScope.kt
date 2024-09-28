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
import jni.jstring
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CVariable
import kotlinx.cinterop.ExperimentalForeignApi

value class JniScope(val env: JniEnvironment) {
    fun jstring.toKString(): String = toKString(env)
    fun String.toJString(): jstring = toJString(env)

    val VisibilityProvider.visibility: JvmVisibility
        get() = getVisibility(env)

    fun AnnotationProvider.hasAnnotation(type: Type): Boolean = hasAnnotation(env, type)
    fun AnnotationProvider.getAnnotation(type: Type): JvmObject = getAnnotation(env, type)

    fun JvmClass.registerNativeMethod(
        address: COpaquePointer,
        descriptor: MethodDescriptor,
    ) = registerNativeMethod(env, address, descriptor)

    fun JvmClass.registerNativeMethod(
        address: COpaquePointer,
        closure: MethodDescriptorBuilder.() -> Unit,
    ) = registerNativeMethod(env, address, closure)

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

    val JvmClass.type: Type
        get() = getType(env)
    val JvmClass.componentTypeClass: JvmClass
        get() = getComponentTypeClass(env)
    val JvmClass.fields: List<JvmField>
        get() = getFields(env)
    val JvmClass.methods: List<JvmMethod>
        get() = getMethods(env)

    fun JvmClass.unreflectField(field: JvmObject): JvmField = unreflectField(env, field)
    fun JvmClass.unreflectMethod(method: JvmObject): JvmMethod = unreflectMethod(env, method)

    fun JvmField.getByte(instance: JvmObject = JvmObject.NULL): Byte = getByte(env, instance)
    fun JvmField.getShort(instance: JvmObject = JvmObject.NULL): Short = getShort(env, instance)
    fun JvmField.getInt(instance: JvmObject = JvmObject.NULL): Int = getInt(env, instance)
    fun JvmField.getLong(instance: JvmObject = JvmObject.NULL): Long = getLong(env, instance)
    fun JvmField.getFloat(instance: JvmObject = JvmObject.NULL): Float = getFloat(env, instance)
    fun JvmField.getDouble(instance: JvmObject = JvmObject.NULL): Double = getDouble(env, instance)
    fun JvmField.getBoolean(instance: JvmObject = JvmObject.NULL): Boolean = getBoolean(env, instance)
    fun JvmField.getChar(instance: JvmObject = JvmObject.NULL): Char = getChar(env, instance)
    fun JvmField.getObject(instance: JvmObject = JvmObject.NULL): JvmObject = getObject(env, instance)
    inline fun <reified R> JvmField.get(instance: JvmObject = JvmObject.NULL): R = get<R>(env, instance)

    fun JvmField.setByte(value: Byte, instance: JvmObject = JvmObject.NULL) = setByte(env, value, instance)
    fun JvmField.setShort(value: Short, instance: JvmObject = JvmObject.NULL) = setShort(env, value, instance)
    fun JvmField.setInt(value: Int, instance: JvmObject = JvmObject.NULL) = setInt(env, value, instance)
    fun JvmField.setLong(value: Long, instance: JvmObject = JvmObject.NULL) = setLong(env, value, instance)
    fun JvmField.setFloat(value: Float, instance: JvmObject = JvmObject.NULL) = setFloat(env, value, instance)
    fun JvmField.setDouble(value: Double, instance: JvmObject = JvmObject.NULL) = setDouble(env, value, instance)
    fun JvmField.setBoolean(value: Boolean, instance: JvmObject = JvmObject.NULL) = setBoolean(env, value, instance)
    fun JvmField.setChar(value: Char, instance: JvmObject = JvmObject.NULL) = setChar(env, value, instance)
    fun JvmField.setObject(value: JvmObject, instance: JvmObject = JvmObject.NULL) = setObject(env, value, instance)
    inline fun <reified R> JvmField.set(value: R, instance: JvmObject = JvmObject.NULL) = set<R>(env, value, instance)
    // @formatter:on

    val JvmField.instance: JvmObject
        get() = getInstance(env)

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

    fun JvmMethod.callChar(
        instance: JvmObject = JvmObject.NULL,
        closure: ArgumentScope.() -> Unit = {}
    ): Char = callChar(env, instance, closure)

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

    val JvmObject.typeClass: JvmClass
        get() = getTypeClass(env)

    @UnsafeJniApi
    fun JvmObject.createGlobalRef(): JvmObjectRef = createGlobalRef(env)

    @UnsafeJniApi
    fun JvmObject.createLocalRef(): JvmObjectRef = createLocalRef(env)

    @UnsafeJniApi
    fun JvmObject.createWeakRef(): JvmObjectRef = createWeakRef(env)

    @UnsafeJniApi
    fun JvmObjectRef.delete() = delete(env)

    fun JvmObject.cast(type: Type): JvmObject = cast(env, type)
    fun JvmObject.cast(clazz: JvmClass): JvmObject = cast(env, clazz)
    fun JvmObject.isInstance(type: Type): Boolean = isInstance(env, type)
    fun JvmObject.isInstance(clazz: JvmClass): Boolean = isInstance(env, clazz)
    fun JvmObject.toKString(): String = toKString(env)

    inline fun <reified T : JvmObject> JvmObject.cast(): T = cast<T>(env)

    fun JvmString.Companion.create(value: String): JvmString = create(env, value)

    val JvmString.value: String?
        get() = get(env)
    val JvmString.length: Int
        get() = getLength(env)

    @UnsafeJniApi
    inline fun <reified T : CPointed> JvmArrayHandle.pin(): CPointer<T> = pin(env)

    @UnsafeJniApi
    fun JvmArrayHandle.unpin(address: COpaquePointer) = unpin(env, address)

    @UnsafeJniApi
    inline fun <reified T : CPointed, reified R> JvmArrayHandle.usePinned(closure: (CPointer<T>) -> R): R =
        usePinned<T, R>(env, closure)

    @UnsafeJniApi
    fun JvmArray.copyPrimitiveDataFrom(from: COpaquePointer, elementSize: Int, range: IntRange) =
        copyPrimitiveDataFrom(env, from, elementSize, range)

    @UnsafeJniApi
    fun JvmArray.copyPrimitiveDataTo(to: COpaquePointer, elementSize: Int, range: IntRange) =
        copyPrimitiveDataTo(env, to, elementSize, range)

    @UnsafeJniApi
    inline fun <reified T : CVariable> JvmArray.copyPrimitiveDataFrom(
        from: COpaquePointer,
        range: IntRange
    ) = copyPrimitiveDataFrom<T>(env, from, range)

    @UnsafeJniApi
    inline fun <reified T : CVariable> JvmArray.copyPrimitiveDataTo(
        to: COpaquePointer,
        range: IntRange
    ) = copyPrimitiveDataTo<T>(env, to, range)

    fun JvmArray.setByte(index: Int, value: Byte) = setByte(env, index, value)
    fun JvmArray.setShort(index: Int, value: Short) = setShort(env, index, value)
    fun JvmArray.setInt(index: Int, value: Int) = setInt(env, index, value)
    fun JvmArray.setLong(index: Int, value: Long) = setLong(env, index, value)
    fun JvmArray.setFloat(index: Int, value: Float) = setFloat(env, index, value)
    fun JvmArray.setDouble(index: Int, value: Double) = setDouble(env, index, value)
    fun JvmArray.setBoolean(index: Int, value: Boolean) = setBoolean(env, index, value)
    fun JvmArray.setChar(index: Int, value: Char) = setChar(env, index, value)
    fun JvmArray.setObject(index: Int, value: JvmObject) = setObject(env, index, value)

    inline operator fun <reified R> JvmArray.set(index: Int, value: R) = set<R>(env, index, value)

    fun JvmArray.getByte(index: Int): Byte = getByte(env, index)
    fun JvmArray.getShort(index: Int): Short = getShort(env, index)
    fun JvmArray.getInt(index: Int): Int = getInt(env, index)
    fun JvmArray.getLong(index: Int): Long = getLong(env, index)
    fun JvmArray.getFloat(index: Int): Float = getFloat(env, index)
    fun JvmArray.getDouble(index: Int): Double = getDouble(env, index)
    fun JvmArray.getBoolean(index: Int): Boolean = getBoolean(env, index)
    fun JvmArray.getChar(index: Int): Char = getChar(env, index)
    fun JvmArray.getObject(index: Int): JvmObject = getObject(env, index)

    inline operator fun <reified R> JvmArray.get(index: Int): R = get<R>(env, index)

    fun JvmArray.toByteArray(): ByteArray = toByteArray(env)
    fun JvmArray.toShortArray(): ShortArray = toShortArray(env)
    fun JvmArray.toIntArray(): IntArray = toIntArray(env)
    fun JvmArray.toLongArray(): LongArray = toLongArray(env)
    fun JvmArray.toFloatArray(): FloatArray = toFloatArray(env)
    fun JvmArray.toDoubleArray(): DoubleArray = toDoubleArray(env)
    fun JvmArray.toBooleanArray(): BooleanArray = toBooleanArray(env)
    fun JvmArray.toCharArray(): CharArray = toCharArray(env)
    fun JvmArray.toObjectArray(): Array<JvmObject> = toObjectArray(env)

    val JvmArray.componentTypeClass: JvmClass
        get() = getComponentTypeClass(env)
    val JvmArray.length: Int
        get() = getLength(env)
    val JvmArray.indices: IntRange
        get() = 0..<getLength(env)
}

@OptIn(UnsafeJniApi::class)
inline fun <reified R> jniScoped(scope: JniScope.() -> R): R {
    val result =
        scope(JniScope(requireNotNull(JniPlatform.attach()) { "Could not access JNI environment" }))
    JniPlatform.detach()
    return result
}

inline fun <reified R> jniScoped(env: JniEnvironment, scope: JniScope.() -> R): R {
    return scope(JniScope(env))
}