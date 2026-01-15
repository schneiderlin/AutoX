plugins {
    id("com.android.library")
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
        minSdk = versions.mini
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = false
    }

    namespace = "com.autocljs"
}

dependencies {
    // Core Android dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${kotlin_version}")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Rhino JavaScript engine for executing compiled JavaScript code
    implementation("org.mozilla:rhino:1.7.14")
    
    // Javet (V8/Node.js) for ES module support
    implementation("com.caoccao.javet:javet-node-android:5.0.2")
    
    // For ClojureScript integration (will be added when we integrate ClojureScript)
    // For now, we'll port the core interfaces and structures
    
    // Dependencies from common module (minimal - only what's needed)
    // We'll add these as we port components
}

