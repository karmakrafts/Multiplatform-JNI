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
import jni.JNI_FALSE
import jni.JNI_TRUE
import jni.jboolean
import jni.jobject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr

fun jboolean.toKBoolean(): Boolean = this == JNI_TRUE.convert<jboolean>()
fun Boolean.toJBoolean(): jboolean =
    if (this) JNI_TRUE.convert<jboolean>() else JNI_FALSE.convert<jboolean>()

@Suppress("UNCHECKED_CAST")
fun <O : jobject> O.newGlobalRefOrNull(env: JNIEnvVar): O? =
    env.pointed?.NewGlobalRef?.invoke(env.ptr, this) as? O?

fun <O : jobject> O.newGlobalRef(env: JNIEnvVar): O =
    requireNotNull(newGlobalRefOrNull(env)) { "Could not create global reference" }

fun <O : jobject> O.deleteGlobalRef(env: JNIEnvVar) =
    env.pointed?.DeleteGlobalRef?.invoke(env.ptr, this)

@Suppress("UNCHECKED_CAST")
fun <O : jobject> O.newLocalRefOrNull(env: JNIEnvVar): O? =
    env.pointed?.NewLocalRef?.invoke(env.ptr, this) as? O?

fun <O : jobject> O.newLocalRef(env: JNIEnvVar): O =
    requireNotNull(newLocalRefOrNull(env)) { "Could not create local reference" }

fun <O : jobject> O.deleteLocalRef(env: JNIEnvVar) =
    env.pointed?.DeleteLocalRef?.invoke(env.ptr, this)