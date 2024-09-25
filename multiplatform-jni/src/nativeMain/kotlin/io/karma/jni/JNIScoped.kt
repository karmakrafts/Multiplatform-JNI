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
import jni.jclass
import jni.jfieldID
import jni.jmethodID
import jni.jobject
import jni.jstring
import kotlinx.cinterop.ExperimentalForeignApi

value class JNIEnvScope(val env: JNIEnvVar) {
    fun jstring.toKString(): String = toKString(env)
    fun String.toJString(): jstring = toJString(env)

    fun findClassOrNull(name: ClassName): jclass? = findClassOrNull(env, name)
    fun findClass(name: ClassName): jclass = findClass(env, name)

    fun jclass.findFieldOrNull(descriptor: FieldDescriptor): jfieldID? =
        findFieldOrNull(env, descriptor)

    fun jclass.findFieldOrNull(descriptor: FieldDescriptorBuilder.() -> Unit): jfieldID? =
        findFieldOrNull(env, descriptor)

    fun jclass.findField(descriptor: FieldDescriptor): jfieldID =
        findField(env, descriptor)

    fun jclass.findField(descriptor: FieldDescriptorBuilder.() -> Unit): jfieldID =
        findField(env, descriptor)

    fun jclass.findMethodOrNull(descriptor: MethodDescriptor): jmethodID? =
        findMethodOrNull(env, descriptor)

    fun jclass.findMethodOrNull(descriptor: MethodDescriptorBuilder.() -> Unit): jmethodID? =
        findMethodOrNull(env, descriptor)

    fun jclass.findMethod(descriptor: MethodDescriptor): jmethodID =
        findMethod(env, descriptor)

    fun jclass.findMethod(descriptor: MethodDescriptorBuilder.() -> Unit): jmethodID =
        findMethod(env, descriptor)

    inline operator fun <reified R> jmethodID.invoke(instance: jobject, vararg args: Any?): R? {
        return invoke<R>(env, instance, *args)
    }

    inline fun <reified R> whileDetached(scope: () -> R): R {
        detachThreadFromVm()
        val result = scope()
        attachThreadToVm()
        return result
    }
}

inline fun <reified R> jniScoped(scope: JNIEnvScope.() -> R): R {
    val result =
        scope(JNIEnvScope(requireNotNull(attachThreadToVm()) { "Could not access JNI environment" }))
    detachThreadFromVm()
    return result
}