# Build and Test Guide for Phase 1 & 2

## Overview

`app1` is a **library module**, so we need to create a test application to use it. This guide shows you how to build and test Phase 1 and Phase 2 functionality.

## Option 1: Create a Simple Test App (Recommended)

### Step 1: Create Test App Module

Create a new module `app1test` that depends on `app1`:

**File: `app1test/build.gradle.kts`**
```kotlin
plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = versions.compile

    defaultConfig {
        applicationId = "com.autocljs.test"
        minSdk = versions.mini
        targetSdk = versions.compile
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    namespace = "com.autocljs.test"
}

dependencies {
    implementation(project(":app1"))
    implementation("androidx.appcompat:appcompat:1.6.1")
}
```

### Step 2: Add to settings.gradle.kts

Add this line:
```kotlin
include(":app1test")
```

### Step 3: Create Test Activity

**File: `app1test/src/main/java/com/autocljs/test/TestActivity.kt`**
```kotlin
package com.autocljs.test

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.autocljs.accessibility.AccessibilityBridgeImpl
import com.autocljs.accessibility.AccessibilityConfig
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.util.ScreenMetrics
import com.autocljs.util.UiHandler

class TestActivity : AppCompatActivity() {
    private lateinit var logView: TextView
    private lateinit var runtime: ScriptRuntime
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        logView = TextView(this).apply {
            text = "AutoCLJS Test App\n\nTap buttons to test Phase 1 & 2"
        }
        
        val testPhase1Btn = Button(this).apply {
            text = "Test Phase 1 (Console)"
            setOnClickListener { testPhase1() }
        }
        
        val testPhase2Btn = Button(this).apply {
            text = "Test Phase 2 (Click)"
            setOnClickListener { testPhase2() }
        }
        
        setContentView(android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(logView)
            addView(testPhase1Btn)
            addView(testPhase2Btn)
        })
        
        runtime = ScriptRuntime(this)
    }
    
    private fun testPhase1() {
        log("Testing Phase 1: Console API")
        runtime.console.log("Hello from Phase 1!")
        runtime.console.info("Info message")
        runtime.console.warn("Warning message")
        runtime.console.error("Error message")
        log("Phase 1 test complete - check logcat")
    }
    
    private fun testPhase2() {
        log("Testing Phase 2: Automation")
        try {
            ScreenMetrics.initIfNeeded(this)
            val config = AccessibilityConfig()
            val uiHandler = UiHandler(this)
            val bridge = AccessibilityBridgeImpl(this, config, uiHandler)
            
            bridge.ensureServiceEnabled()
            runtime.initAutomation(bridge)
            
            val automator = runtime.getAutomator()
            if (automator != null) {
                log("Automator initialized")
                log("Clicking at (500, 500)...")
                val success = automator.click(500, 500)
                log("Click result: $success")
            } else {
                log("ERROR: Automator is null")
            }
        } catch (e: Exception) {
            log("ERROR: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun log(message: String) {
        runOnUiThread {
            logView.text = "${logView.text}\n$message"
        }
        android.util.Log.d("TestActivity", message)
    }
}
```

### Step 4: Create AndroidManifest

**File: `app1test/src/main/AndroidManifest.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
    
    <application
        android:allowBackup="true"
        android:icon="@android:drawable/ic_dialog_info"
        android:label="AutoCLJS Test"
        android:theme="@android:style/Theme.Material.Light">
        
        <activity
            android:name=".TestActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- Accessibility Service -->
        <service
            android:name="com.autocljs.accessibility.AccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
    </application>
</manifest>
```

**Note**: The accessibility service config XML is in `app1/src/main/res/xml/`, so we need to reference it. We may need to copy it or reference it properly.

## Option 2: Use Existing App Module (Quick Test)

If you want to test quickly without creating a new module, you can:

1. Add `app1` as a dependency to the existing `app` module
2. Create a test activity in the `app` module
3. Add the accessibility service to `app/src/main/AndroidManifest.xml`

## Build Instructions

### Using Gradle

```bash
# Build the library
./gradlew :app1:assembleDebug

# Build test app (if using Option 1)
./gradlew :app1test:assembleDebug

# Install on device
./gradlew :app1test:installDebug
```

### Using Android Studio

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Select `app1test` (or your test app) from the run configuration
4. Click Run (or press Shift+F10)

## Testing Steps

### Phase 1 Testing

1. Launch the test app
2. Tap "Test Phase 1 (Console)"
3. Check logcat for console output:
   ```
   adb logcat | grep "AutoCLJS"
   ```

### Phase 2 Testing

1. **Enable Accessibility Service First**:
   - Go to Android Settings → Accessibility
   - Find "AutoCLJS automation service" (or your service name)
   - Enable it

2. Launch the test app
3. Tap "Test Phase 2 (Click)"
4. The app should click at coordinates (500, 500)
5. Check logcat for results

### Troubleshooting

**Issue**: "Accessibility service not enabled"
- **Solution**: Enable the service in Android Settings → Accessibility

**Issue**: "Automator is null"
- **Solution**: Check that `initAutomation()` was called before `getAutomator()`

**Issue**: Build fails with "cannot find symbol"
- **Solution**: Make sure `app1` module is included in `settings.gradle.kts`

**Issue**: Accessibility service config not found
- **Solution**: Copy `app1/src/main/res/xml/accessibility_service_config.xml` to your test app's res folder, or ensure proper resource merging

## Quick Test Script

You can also create a simple test script to verify everything works:

```kotlin
// Quick test in TestActivity
fun quickTest() {
    // Phase 1
    runtime.console.log("Phase 1: Console works!")
    
    // Phase 2
    try {
        val bridge = AccessibilityBridgeImpl(
            this,
            AccessibilityConfig(),
            UiHandler(this)
        )
        bridge.ensureServiceEnabled()
        runtime.initAutomation(bridge)
        runtime.getAutomator()?.click(500, 500)
        runtime.console.log("Phase 2: Click executed!")
    } catch (e: Exception) {
        runtime.console.error("Phase 2 failed: ${e.message}")
    }
}
```

## Next Steps

After successful testing:
1. Verify Phase 1 console output works
2. Verify Phase 2 clicks work (enable accessibility service first!)
3. Test on different screen sizes
4. Test error handling (disable service, test error messages)

