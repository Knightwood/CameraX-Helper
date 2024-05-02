plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("maven-publish")
}

android {
    compileSdk = 34
    namespace = "com.kiylx.camerax_lib"

    defaultConfig {
        minSdk = 23
        targetSdk = 32
//        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            //设置支持的SO库架构（开发者可以根据需要，选择一个或多个平台的so）
//            abiFilters += listOf("armeabi", "armeabi-v7a", "arm64-v8a")
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}


dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar","*.aar"))))
    // Kotlin Lang
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // App compat and UI things
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.9.0")

    api("com.github.Knightwood:SimpleStore:1.0")

    // CameraX core library
    val camerax_version ="1.3.0"

    // CameraX Camera2 extensions
    api ("androidx.camera:camera-camera2:$camerax_version")
    // CameraX Lifecycle library
    api ("androidx.camera:camera-lifecycle:$camerax_version")
    // If you want to additionally use the CameraX VideoCapture library
    api ("androidx.camera:camera-video:$camerax_version")
    // If you want to additionally use the CameraX View class
    api ("androidx.camera:camera-view:$camerax_version")
    // If you want to additionally add CameraX ML Kit Vision Integration
    api ("androidx.camera:camera-mlkit-vision:1.4.0-alpha02")
    // If you want to additionally use the CameraX Extensions library
    api ("androidx.camera:camera-extensions:$camerax_version")
    api("com.blankj:utilcodex:1.31.0")
    api("com.google.mlkit:face-detection:16.1.5")
    api("com.guolindev.permissionx:permissionx:1.6.1")

    api("org.tensorflow:tensorflow-lite-api:2.9.0")
    api("org.tensorflow:tensorflow-lite:2.9.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.knightwood"
                artifactId = "SimpleCameraX"
                version = rootProject.ext["version"].toString()
                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
}