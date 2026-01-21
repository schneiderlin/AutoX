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
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    lint {
        abortOnError = false
    }
    
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
    
    // Ktor WebSocket client
    implementation(libs.bundles.ktor)
    
    // Gson for JSON parsing
    implementation(libs.google.gson)
    
    // Compose for WebSocketTestActivity UI
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // For ClojureScript integration (will be added when we integrate ClojureScript)
    // For now, we'll port the core interfaces and structures
    
    // Dependencies from common module (minimal - only what's needed)
    // We'll add these as we port components
}

