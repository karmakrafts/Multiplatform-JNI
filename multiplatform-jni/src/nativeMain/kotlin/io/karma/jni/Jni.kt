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

import jni.JNI_FALSE
import jni.JNI_TRUE
import jni.jboolean
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert

fun jboolean.toKBoolean(): Boolean = this == JNI_TRUE.convert<jboolean>()
fun Boolean.toJBoolean(): jboolean =
    if (this) JNI_TRUE.convert<jboolean>() else JNI_FALSE.convert<jboolean>()

enum class JvmVisibility(internal val jvmValue: UShort) {
    // @formatter:off
    PUBLIC   (0x1U),
    PRIVATE  (0x2U),
    PROTECTED(0x4U)
    // @formatter:on
}