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

import org.jetbrains.kotlin.konan.target.KonanTarget
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
}

val jniFiles = projectDir.toPath() / "jni"

kotlin {
    jvm()
    mingwX64 {
        val jniHome = jniFiles / "windows-x64"
        compilations["main"].cinterops {
            val jni by creating {
                compilerOpts("-I${jniHome / "include" / "win32"}")
                headers("${jniHome / "include" / "jni.h"}")
            }
        }
        binaries {
            sharedLib {
                linkerOpts(
                    "-L${jniHome / "lib"}",
                    "-ljawt",
                    "-ljvm"
                )
            }
        }
    }
    listOf(linuxX64(), linuxArm64()).forEach { target ->
        val platformPair = when (target.konanTarget) {
            KonanTarget.LINUX_X64 -> "linux-x64"
            KonanTarget.LINUX_ARM64 -> "linux-arm64"
            else -> throw IllegalStateException("Unsupported target platform")
        }
        val jniHome = jniFiles / platformPair
        target.apply {
            compilations["main"].cinterops {
                val jni by creating {
                    compilerOpts("-I${jniHome / "include" / "linux"}")
                    headers("${jniHome / "include" / "jni.h"}")
                }
            }
            binaries {
                sharedLib {
                    linkerOpts(
                        "-L${jniHome / "lib"}",
                        "-llibjawt.so",
                        "-llibjvm.so"
                    )
                }
            }
        }
    }
    listOf(macosX64(), macosArm64()).forEach { target ->
        val platformPair = when (target.konanTarget) {
            KonanTarget.MACOS_X64 -> "macos-x64"
            KonanTarget.MACOS_ARM64 -> "macos-arm64"
            else -> throw IllegalStateException("Unsupported target platform")
        }
        val jniHome = jniFiles / platformPair
        target.apply {
            compilations["main"].cinterops {
                val jni by creating {
                    compilerOpts("-I${jniHome / "include" / "darwin"}")
                    headers("${jniHome / "include" / "jni.h"}")
                }
            }
            binaries {
                framework {
                    baseName = "MultiplatformJNI"
                }
                sharedLib {
                    linkerOpts(
                        "-L${jniHome / "lib"}",
                        "-llibjawt.dylib",
                        "-llibjvm.dylib"
                    )
                }
            }
        }
    }
}