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
import kotlin.io.path.div

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.dokka)
    `maven-publish`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val jniFiles = projectDir.toPath() / "jni"

kotlin {
    jvm()
    mingwX64 {
        val jniHome = jniFiles / "windows-x64"
        val dlfcnHome = projectDir.toPath() / "mingw-dlfcn"
        compilations["main"].cinterops {
            val dlfcn by creating {
                compilerOpts("-I${dlfcnHome / "include"}")
                headers("${dlfcnHome / "include" / "dlfcn.h"}")
            }
            val jni by creating {
                compilerOpts("-I${jniHome / "include"}", "-I${jniHome / "include" / "win32"}")
                headers("${jniHome / "include" / "jni.h"}")
            }
        }
        binaries {
            sharedLib {
                linkerOpts(
                    "-L${jniHome / "lib"}",
                    "-L${dlfcnHome / "lib"}",
                    "-ljawt",
                    "-ljvm",
                    "-lssp",
                    "-ldl"
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
                val dlfcn by creating {
                    compilerOpts("-I/usr/include")
                    headers("/usr/include/dlfcn.h")
                }
                val jni by creating {
                    compilerOpts("-I${jniHome / "include"}", "-I${jniHome / "include" / "linux"}")
                    headers("${jniHome / "include" / "jni.h"}")
                }
            }
            binaries {
                sharedLib {
                    linkerOpts("-L/usr/lib", "-ldl")
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
                val dlfcn by creating {
                    compilerOpts("-I/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include")
                    headers("/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/dlfcn.h")
                }
                val jni by creating {
                    compilerOpts("-I${jniHome / "include"}", "-I${jniHome / "include" / "darwin"}")
                    headers("${jniHome / "include" / "jni.h"}")
                }
            }
            binaries {
                framework {
                    baseName = "MultiplatformJNI"
                }
            }
        }
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.stately.concurrency)
                implementation(libs.stately.collections)
            }
        }
    }
}

val dokkaJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

tasks {
    dokkaHtml {
        dokkaSourceSets.create("main") {
            reportUndocumented = false
            jdkVersion = java.toolchain.languageVersion.get().asInt()
            noAndroidSdkLink = true
            externalDocumentationLink("https://docs.karmakrafts.dev/multiplatform-jni")
        }
    }
    System.getProperty("publishDocs.root")?.let { docsDir ->
        create<Copy>("publishDocs") {
            mustRunAfter(dokkaJar)
            from(zipTree(dokkaJar.get().outputs.files.first()))
            into(docsDir)
        }
    }
}

publishing {
    System.getenv("CI_API_V4_URL")?.let { apiUrl ->
        repositories {
            maven {
                url = uri("$apiUrl/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
                name = "GitLab"
                credentials(HttpHeaderCredentials::class) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
                authentication {
                    create("header", HttpHeaderAuthentication::class)
                }
            }
        }
    }
    publications.configureEach {
        if (this is MavenPublication) {
            artifact(dokkaJar)
            pom {
                name = project.name
                description =
                    "Multiplatform bindings for the native JNI API on Linux, Windows and macOS."
                url = System.getenv("CI_PROJECT_URL")
                licenses {
                    license {
                        name = "Apache License 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "kitsunealex"
                        name = "KitsuneAlex"
                        url = "https://git.karmakrafts.dev/KitsuneAlex"
                    }
                }
                scm {
                    url = this@pom.url
                }
            }
        }
    }
}