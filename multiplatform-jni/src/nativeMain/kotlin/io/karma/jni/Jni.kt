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
import jni.JavaVMVar
import jni.jboolean
import jni.jbyte
import jni.jclass
import jni.jdouble
import jni.jfieldID
import jni.jfloat
import jni.jint
import jni.jlong
import jni.jmethodID
import jni.jobject
import jni.jshort
import jni.jstring
import jni.jvalue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert

const val JNI_OK: JvmInt = jni.JNI_OK
const val JNI_ERR: JvmInt = jni.JNI_ERR
const val JNI_TRUE: JvmInt = jni.JNI_TRUE
const val JNI_FALSE: JvmInt = jni.JNI_FALSE

typealias JavaVm = JavaVMVar
typealias JniEnvironment = JNIEnvVar

typealias JvmByte = jbyte
typealias JvmShort = jshort
typealias JvmInt = jint
typealias JvmLong = jlong
typealias JvmFloat = jfloat
typealias JvmDouble = jdouble
typealias JvmBoolean = jboolean

typealias JvmClassHandle = jclass
typealias JvmObjectHandle = jobject
typealias JvmStringHandle = jstring
typealias JvmValue = jvalue
typealias JvmFieldId = jfieldID
typealias JvmMethodId = jmethodID

fun JvmBoolean.toKBoolean(): Boolean = this == JNI_TRUE.convert<JvmBoolean>()
fun Boolean.toJBoolean(): JvmBoolean =
    if (this) JNI_TRUE.convert<JvmBoolean>() else JNI_FALSE.convert<JvmBoolean>()

enum class JvmVisibility(internal val jvmValue: UShort) {
    // @formatter:off
    PUBLIC   (0x1U),
    PRIVATE  (0x2U),
    PROTECTED(0x4U)
    // @formatter:on
}