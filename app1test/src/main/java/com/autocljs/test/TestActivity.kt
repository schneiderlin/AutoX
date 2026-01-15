package com.autocljs.test

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.autocljs.ScriptEngineService
import com.autocljs.accessibility.AccessibilityBridgeImpl
import com.autocljs.accessibility.AccessibilityConfig
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.script.StringScriptSource
import com.autocljs.util.ScreenMetrics
import com.autocljs.util.UiHandler

/**
 * Test Activity for Phase 1, 2, 3, and 4.
 * 
 * Phase 1: Tests Console API (JavaScript execution)
 * Phase 2: Tests Automation (Click) via JavaScript
 * Phase 3: Auto API exposed to JavaScript
 * Phase 4: TestActivity executes JavaScript code
 */
class TestActivity : AppCompatActivity() {
    
    private lateinit var logView: TextView
    private lateinit var runtime: ScriptRuntime
    private lateinit var scriptEngineService: ScriptEngineService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize runtime
        runtime = ScriptRuntime(this)
        
        // Initialize script engine service with shared runtime
        // This ensures the engine uses the same runtime instance where automation is initialized
        scriptEngineService = ScriptEngineService.Builder(this)
            .setRuntime(runtime)
            .build()
        
        // Create UI
        logView = TextView(this).apply {
            text = "AutoCLJS Test App\n\nTap buttons to test JavaScript execution"
            textSize = 14f
            setPadding(16, 16, 16, 16)
        }
        
        val scrollView = ScrollView(this).apply {
            addView(logView)
        }
        
        val testPhase1Btn = Button(this).apply {
            text = "Test Phase 1 (Console JS)"
            setOnClickListener { testPhase1() }
        }
        
        val testPhase2Btn = Button(this).apply {
            text = "Test Phase 2 (Click JS)"
            setOnClickListener { testPhase2() }
        }
        
        val testPhase2MultipleBtn = Button(this).apply {
            text = "Test Phase 2 (Multiple Clicks JS)"
            setOnClickListener { testPhase2Multiple() }
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(scrollView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ))
            addView(testPhase1Btn)
            addView(testPhase2Btn)
            addView(testPhase2MultipleBtn)
        }
        
        setContentView(layout)
    }
    
    private fun testPhase1() {
        log("=== Testing Phase 1: Console API (JavaScript) ===")
        
        // Execute JavaScript code that uses console API
        val jsCode = """
            console.log("Hello from JavaScript!");
            console.info("This is an info message");
            console.warn("This is a warning message");
            console.error("This is an error message");
            console.log("Phase 1 test complete!");
        """.trimIndent()
        
        try {
            val source = StringScriptSource("test_phase1.js", jsCode)
            val execution = scriptEngineService.execute(source)
            
            val exception = execution.exception
            if (exception != null) {
                log("ERROR: ${exception.message}")
                exception.printStackTrace()
            } else {
                log("JavaScript executed successfully!")
                log("Check logcat for console output (tag: 'AutoCLJS')")
            }
        } catch (e: Exception) {
            log("ERROR executing JavaScript: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun testPhase2() {
        log("=== Testing Phase 2: Automation (Single Click via JavaScript) ===")
        
        // Run on background thread to avoid blocking UI
        Thread {
            try {
                // Initialize screen metrics
                ScreenMetrics.initIfNeeded(this)
                log("Screen metrics initialized")
                
                // Create accessibility bridge
                val config = AccessibilityConfig()
                val uiHandler = UiHandler(this)
                val bridge = AccessibilityBridgeImpl(this, config, uiHandler)
                log("Accessibility bridge created")
                
                // Ensure service is enabled
                log("Checking accessibility service...")
                bridge.ensureServiceEnabled()
                log("Accessibility service is enabled ✓")
                
                // Initialize automation (required before JavaScript can use auto API)
                runtime.initAutomation(bridge)
                log("Automation initialized")
                
                // Execute JavaScript code that uses auto.click()
                val jsCode = """
                    console.log("Testing automation from JavaScript...");
                    console.log("Clicking at coordinates (500, 500)...");
                    var result = auto.click(500, 500);
                    console.log("Click result: " + result);
                    if (result) {
                        console.log("SUCCESS ✓");
                    } else {
                        console.log("FAILED ✗");
                    }
                """.trimIndent()
                
                val source = StringScriptSource("test_phase2.js", jsCode)
                val execution = scriptEngineService.execute(source)
                
                val exception = execution.exception
                if (exception != null) {
                    log("ERROR: ${exception.message}")
                    exception.printStackTrace()
                } else {
                    log("JavaScript executed successfully!")
                    log("Check logcat for console output")
                }
            } catch (e: IllegalStateException) {
                log("ERROR: ${e.message}")
                log("Please enable the accessibility service in Android Settings:")
                log("Settings → Accessibility → AutoCLJS automation service")
            } catch (e: Exception) {
                log("ERROR: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }
    
    private fun testPhase2Multiple() {
        log("=== Testing Phase 2: Automation (Multiple Clicks via JavaScript) ===")
        
        // Run on background thread to avoid blocking UI
        Thread {
            try {
                ScreenMetrics.initIfNeeded(this)
                val config = AccessibilityConfig()
                val uiHandler = UiHandler(this)
                val bridge = AccessibilityBridgeImpl(this, config, uiHandler)
                bridge.ensureServiceEnabled()
                runtime.initAutomation(bridge)
                
                // Execute JavaScript code that performs multiple clicks
                val jsCode = """
                    console.log("Performing multiple clicks from JavaScript...");
                    
                    console.log("Click 1 at (100, 100)...");
                    var result1 = auto.click(100, 100);
                    console.log("Click 1 result: " + result1);
                    
                    // Note: JavaScript doesn't have Thread.sleep, but we can use a simple delay
                    // For now, just log the clicks sequentially
                    console.log("Click 2 at (200, 200)...");
                    var result2 = auto.click(200, 200);
                    console.log("Click 2 result: " + result2);
                    
                    console.log("Long click at (300, 300)...");
                    var result3 = auto.longClick(300, 300);
                    console.log("Long click result: " + result3);
                    
                    console.log("Multiple clicks test complete ✓");
                """.trimIndent()
                
                val source = StringScriptSource("test_phase2_multiple.js", jsCode)
                val execution = scriptEngineService.execute(source)
                
                val exception = execution.exception
                if (exception != null) {
                    log("ERROR: ${exception.message}")
                    exception.printStackTrace()
                } else {
                    log("JavaScript executed successfully!")
                    log("Check logcat for console output")
                }
            } catch (e: IllegalStateException) {
                log("ERROR: ${e.message}")
                log("Please enable the accessibility service in Android Settings")
            } catch (e: Exception) {
                log("ERROR: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }
    
    private fun log(message: String) {
        runOnUiThread {
            logView.text = "${logView.text}\n$message"
        }
        android.util.Log.d("TestActivity", message)
    }
}

