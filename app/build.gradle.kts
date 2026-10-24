import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.beryu.tvhome"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.beryu.tvhome"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "1.0.9"
        testInstrumentationRunner = "com.beryu.tvhome.SmokeInstrumentation"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint { abortOnError = true }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
