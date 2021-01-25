import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.firebase.crashlytics")
    //id("com.google.gms.google-services")
}

android {
    compileSdkVersion(29)

    defaultConfig {
        applicationId = "com.bw.vrtnumm.androidApp"
        minSdkVersion(22)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"

        buildConfigField("Boolean", "VIDEO_PREVIEWS", "false")
        buildConfigField("Boolean", "USE_HYBRID", "true")
        buildConfigField("Boolean", "USE_FIREBASE", "false")
    }

    buildFeatures {
        compose = true
    }

    dexOptions {
        javaMaxHeapSize = "3g"
    }

    composeOptions {
        kotlinCompilerVersion = "1.4.21"
        kotlinCompilerExtensionVersion = Versions.compose
    }

//    signingConfigs {
//        create("release") {
//            val propsFile = File(rootDir, "key.properties")
//            val props = Properties()
//                props.load(propsFile.inputStream())
//            keyAlias = props["keyAlias"] as String
//            keyPassword = props["keyPassword"] as String
//            storeFile = file(props["storeFile"] as String)
//            storePassword = props["storePassword"] as String
//        }
//    }
//
//    buildTypes {
//        getByName("release") {
//            signingConfig = signingConfigs.getByName("release")
//            isMinifyEnabled = true
//            isShrinkResources = true
//            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
//        }
//    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = listOf("-Xallow-jvm-ir-dependencies", "-Xskip-prerelease-check",
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("com.google.android.material:material:1.2.1")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation( "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.3")
    implementation( "androidx.lifecycle:lifecycle-runtime-ktx:2.3.0-beta01")
    implementation( "androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation( "androidx.lifecycle:lifecycle-livedata-ktx:2.2.0")
    implementation( "androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")

    implementation("androidx.compose.runtime:runtime:${Versions.compose}")
    implementation("androidx.compose.ui:ui:${Versions.compose}")
    implementation("androidx.compose.foundation:foundation-layout:${Versions.compose}")
    implementation("androidx.compose.material:material:${Versions.compose}")
    implementation("androidx.compose.material:material-icons-extended:${Versions.compose}")
    implementation("androidx.compose.foundation:foundation:${Versions.compose}")
    implementation("androidx.compose.animation:animation:${Versions.compose}")
    implementation("androidx.compose.ui:ui-tooling:${Versions.compose}")
    implementation("androidx.compose.runtime:runtime-livedata:${Versions.compose}")

    val exo_version = "2.12.0"
    implementation("com.google.android.exoplayer:exoplayer-core:$exo_version")
    implementation("com.google.android.exoplayer:exoplayer-ui:$exo_version")
    implementation("com.google.android.exoplayer:exoplayer-dash:$exo_version")

    implementation("dev.chrisbanes.accompanist:accompanist-coil:0.4.0")

    val work_version = "2.3.3"
    implementation("androidx.work:work-runtime:$work_version")
    implementation("androidx.work:work-runtime-ktx:$work_version")

    val lottieVersion = "1.0.0-alpha03"
    implementation("com.airbnb.android:lottie-compose:$lottieVersion")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.0.9")

    implementation(platform("com.google.firebase:firebase-bom:26.1.1"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
}

