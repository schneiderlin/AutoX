package com.autocljs.runtime

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.autocljs.accessibility.AccessibilityBridge
import com.autocljs.automation.SimpleActionAutomator
import com.autocljs.runtime.api.Console

/**
 * Script runtime for Phase 2.
 * Provides runtime environment with Console API and automation.
 */
class ScriptRuntime(private val context: Context) {
    
    val console: Console = com.autocljs.runtime.api.SimpleConsole("AutoCLJS")
    
    private var accessibilityBridge: AccessibilityBridge? = null
    private var automator: SimpleActionAutomator? = null
    
    /**
     * Get the automator for coordinate-based clicks.
     * Will be exposed to JavaScript runtime.
     */
    fun getAutomator(): SimpleActionAutomator? {
        return automator
    }
    
    /**
     * Initialize automation with accessibility bridge.
     */
    fun initAutomation(bridge: AccessibilityBridge) {
        this.accessibilityBridge = bridge
        this.automator = SimpleActionAutomator(bridge) {
            Handler(Looper.getMainLooper())
        }
    }
    
    companion object {
        @Volatile
        private var applicationContext: Context? = null
        
        fun setApplicationContext(context: Context) {
            applicationContext = context.applicationContext
        }
        
        fun getApplicationContext(): Context? {
            return applicationContext
        }
    }
}

