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

    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    implementation(project(":app1"))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${kotlin_version}")
    
    // HTTP client for fetching scripts
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // EnhancedFloaty for floating window support
    implementation("com.github.hyb1996:EnhancedFloaty:0.31")

    // Coroutines and lifecycle support
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

