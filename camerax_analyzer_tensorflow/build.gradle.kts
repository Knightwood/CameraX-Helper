plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")

}

android {
    namespace = "com.kiylx.camera.camerax_analyzer_tensorflow"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            //设置支持的SO库架构（开发者可以根据需要，选择一个或多个平台的so）
//            abiFilters += listOf("armeabi", "armeabi-v7a", "arm64-v8a")
            //noinspection ChromeOsAbiSupport
            abiFilters += rootProject.ext["abi"] as List<String>
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")

    api("org.tensorflow:tensorflow-lite-api:2.9.0")
    api("org.tensorflow:tensorflow-lite:2.9.0")

    compileOnly(project(":camerax_lib"))
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.Knightwood"
                artifactId = "camerax_analyzer_tensorflow"
                version = rootProject.ext["version"].toString()
                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
}