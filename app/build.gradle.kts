plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 34
    namespace = "com.kiylx.cameraxexample"
    defaultConfig {
        applicationId = "com.kiylx.cameraxexample"
        minSdk =23
        targetSdk =34
        versionCode= 1
        versionName= "1.0"

        //        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            //设置支持的SO库架构（开发者可以根据需要，选择一个或多个平台的so）
//            abiFilters += listOf("armeabi", "armeabi-v7a", "arm64-v8a")
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

}

dependencies {

    implementation ("androidx.core:core-ktx:1.8.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(project(":camerax_lib"))
    implementation(project(":camerax_analyzer"))
    implementation(project(":camerax_analyzer_tensorflow"))
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

}