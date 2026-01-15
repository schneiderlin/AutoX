plugins {
    id("com.android.application")
    id("kotlin-android")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(versions.javaVersionInt))
    }
}

android {
    compileSdk = versions.compile

    defaultConfig {
        applicationId = "com.autocljs.test"
        minSdk = versions.mini
        targetSdk = versions.compile
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    namespace = "com.autocljs.test"
}

dependencies {
    implementation(project(":app1"))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${kotlin_version}")
}

