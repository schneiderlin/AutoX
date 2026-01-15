package com.autocljs.test

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.autocljs.accessibility.AccessibilityBridgeImpl
import com.autocljs.accessibility.AccessibilityConfig
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.util.ScreenMetrics
import com.autocljs.util.UiHandler

/**
 * Test Activity for Phase 1 and Phase 2.
 * 
 * Phase 1: Tests Console API
 * Phase 2: Tests Automation (Click)
 */
class TestActivity : AppCompatActivity() {
    
    private lateinit var logView: TextView
    private lateinit var runtime: ScriptRuntime
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize runtime
        runtime = ScriptRuntime(this)
        
        // Create UI
        logView = TextView(this).apply {
            text = "AutoCLJS Test App\n\nTap buttons to test Phase 1 & 2"
            textSize = 14f
            setPadding(16, 16, 16, 16)
        }
        
        val scrollView = ScrollView(this).apply {
            addView(logView)
        }
        
        val testPhase1Btn = Button(this).apply {
            text = "Test Phase 1 (Console)"
            setOnClickListener { testPhase1() }
        }
        
        val testPhase2Btn = Button(this).apply {
            text = "Test Phase 2 (Click at 500,500)"
            setOnClickListener { testPhase2() }
        }
        
        val testPhase2MultipleBtn = Button(this).apply {
            text = "Test Phase 2 (Multiple Clicks)"
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
        log("=== Testing Phase 1: Console API ===")
        runtime.console.log("Hello from Phase 1!")
        runtime.console.info("This is an info message")
        runtime.console.warn("This is a warning message")
        runtime.console.error("This is an error message")
        log("Phase 1 test complete - check logcat for console output")
        log("Look for tag: 'AutoCLJS'")
    }
    
    private fun testPhase2() {
        log("=== Testing Phase 2: Automation (Single Click) ===")
        
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
                
                // Initialize automation
                runtime.initAutomation(bridge)
                log("Automation initialized")
                
                val automator = runtime.getAutomator()
                if (automator != null) {
                    log("Automator ready")
                    log("Clicking at coordinates (500, 500)...")
                    val success = automator.click(500, 500)
                    log("Click result: ${if (success) "SUCCESS ✓" else "FAILED ✗"}")
                } else {
                    log("ERROR: Automator is null")
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
        log("=== Testing Phase 2: Automation (Multiple Clicks) ===")
        
        // Run on background thread to avoid blocking UI
        Thread {
            try {
                ScreenMetrics.initIfNeeded(this)
                val config = AccessibilityConfig()
                val uiHandler = UiHandler(this)
                val bridge = AccessibilityBridgeImpl(this, config, uiHandler)
                bridge.ensureServiceEnabled()
                runtime.initAutomation(bridge)
                
                val automator = runtime.getAutomator()
                if (automator != null) {
                    log("Performing multiple clicks...")
                    automator.click(100, 100)
                    Thread.sleep(500)
                    log("Click 1 at (100, 100) completed")
                    
                    automator.click(200, 200)
                    Thread.sleep(500)
                    log("Click 2 at (200, 200) completed")
                    
                    automator.longClick(300, 300)
                    Thread.sleep(500)
                    log("Long click at (300, 300) completed")
                    
                    log("Multiple clicks test complete ✓")
                } else {
                    log("ERROR: Automator is null")
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

