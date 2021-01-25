import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.21"
    id("com.android.library")
    id("com.squareup.sqldelight")
}

android {
    compileSdkVersion(29)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(22)
        targetSdkVersion(29)
    }
}

android {
    configurations {
        create("androidTestApi")
        create("androidTestDebugApi")
        create("androidTestReleaseApi")
        create("testApi")
        create("testDebugApi")
        create("testReleaseApi")
    }
}

kotlin {
    android()
    ios {
        binaries {
            framework {
                baseName = "shared"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Works as common dependency as well as the platform one
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.kotlinxSerialization}")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}") {
                    isForce = true
                }

                implementation("io.ktor:ktor-client-core:${Versions.ktor}")
                implementation("io.ktor:ktor-client-json:${Versions.ktor}")
                implementation("io.ktor:ktor-client-serialization:${Versions.ktor}")
                implementation("io.ktor:ktor-client-logging:${Versions.ktor}")

                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")

                implementation("com.squareup.sqldelight:runtime:${Versions.sqlDelight}")
                implementation("com.squareup.sqldelight:coroutines-extensions:${Versions.sqlDelight}")

                implementation("com.russhwolf:multiplatform-settings:0.7.1")
                implementation("com.russhwolf:multiplatform-settings-no-arg:0.7.1")
                implementation("co.touchlab:kermit:0.1.8")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.google.android.material:material:1.2.1")
                implementation("io.ktor:ktor-client-cio:${Versions.ktor}")
                implementation("io.ktor:ktor-client-android:${Versions.ktor}")
                implementation("com.squareup.sqldelight:android-driver:${Versions.sqlDelight}")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-ios:${Versions.ktor}")
                implementation("com.squareup.sqldelight:native-driver:${Versions.sqlDelight}")
            }
        }
        val iosTest by getting
    }
}

val packForXcode by tasks.creating(Sync::class) {
    group = "build"
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val sdkName = System.getenv("SDK_NAME") ?: "iphonesimulator"
    val targetName = "ios" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64"
    //val targetName = "iosArm64"
    val framework = kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)
    val targetDir = File(
        buildDir,
        "xcode-frameworks"
    )
    from({ framework.outputDirectory })
    into(targetDir)
}

tasks.getByName("build").dependsOn(packForXcode)

sqldelight {
    database("VrtNuDatabase") {
        packageName = "com.bw.vrtnumm.shared.db"
        sourceFolders = listOf("sqldelight")
    }
}