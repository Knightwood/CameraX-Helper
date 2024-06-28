// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id ("com.android.library") version "8.2.2" apply false
    id ("org.jetbrains.kotlin.android") version "1.9.0" apply false
}

ext {
    this["version"] = "1.3.1"
    this["abi"] = listOf("arm64-v8a") //listOf("armeabi", "armeabi-v7a", "arm64-v8a")
}