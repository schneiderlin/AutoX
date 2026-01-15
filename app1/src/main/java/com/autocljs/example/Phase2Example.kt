package com.autocljs.example

import android.content.Context
import android.util.Log
import com.autocljs.accessibility.AccessibilityBridgeImpl
import com.autocljs.accessibility.AccessibilityConfig
import com.autocljs.automation.SimpleActionAutomator
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.util.ScreenMetrics
import com.autocljs.util.UiHandler

/**
 * Example demonstrating Phase 2: Basic Automation (Click)
 * 
 * This example shows how to:
 * 1. Initialize accessibility bridge
 * 2. Set up automation
 * 3. Perform coordinate-based clicks
 */
object Phase2Example {
    
    private const val TAG = "Phase2Example"
    
    /**
     * Example: Initialize automation and perform a click at coordinates (500, 500)
     */
    fun exampleClick(context: Context) {
        try {
            // Initialize screen metrics
            ScreenMetrics.initIfNeeded(context)
            
            // Create accessibility bridge
            val config = AccessibilityConfig()
            val uiHandler = UiHandler(context)
            val bridge = AccessibilityBridgeImpl(context, config, uiHandler)
            
            // Ensure accessibility service is enabled
            bridge.ensureServiceEnabled()
            
            // Create runtime and initialize automation
            val runtime = ScriptRuntime(context)
            runtime.initAutomation(bridge)
            
            // Get automator
            val automator = runtime.getAutomator()
            if (automator != null) {
                // Perform a click at coordinates (500, 500)
                val success = automator.click(500, 500)
                Log.d(TAG, "Click at (500, 500): $success")
            } else {
                Log.e(TAG, "Automator not initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in exampleClick", e)
        }
    }
    
    /**
     * Example: Perform multiple clicks
     */
    fun exampleMultipleClicks(context: Context) {
        try {
            ScreenMetrics.initIfNeeded(context)
            val config = AccessibilityConfig()
            val uiHandler = UiHandler(context)
            val bridge = AccessibilityBridgeImpl(context, config, uiHandler)
            bridge.ensureServiceEnabled()
            
            val runtime = ScriptRuntime(context)
            runtime.initAutomation(bridge)
            
            val automator = runtime.getAutomator()
            if (automator != null) {
                // Click at different coordinates
                automator.click(100, 100)
                Thread.sleep(500)
                automator.click(200, 200)
                Thread.sleep(500)
                automator.longClick(300, 300)
                Log.d(TAG, "Multiple clicks performed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in exampleMultipleClicks", e)
        }
    }
}

