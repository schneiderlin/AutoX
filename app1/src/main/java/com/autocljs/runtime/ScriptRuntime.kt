package com.autocljs.runtime

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.autocljs.accessibility.AccessibilityBridge
import com.autocljs.automation.SimpleActionAutomator
import com.autocljs.layout.LayoutInspector
import com.autocljs.runtime.api.Console

/**
 * Script runtime for Phase 2.
 * Provides runtime environment with Console API and automation.
 */
class ScriptRuntime(private val context: Context) {
    
    val console: Console = com.autocljs.runtime.api.SimpleConsole("AutoCLJS")
    
    private var accessibilityBridge: AccessibilityBridge? = null
    private var automator: SimpleActionAutomator? = null
    private var layoutInspector: LayoutInspector? = null
    
    // Background thread for gesture callbacks to avoid deadlock
    private var gestureHandlerThread: HandlerThread? = null
    private var gestureHandler: Handler? = null
    
    /**
     * Get the automator for coordinate-based clicks.
     * Will be exposed to JavaScript runtime.
     */
    fun getAutomator(): SimpleActionAutomator? {
        return automator
    }

    /**
     * Get the layout inspector for UI hierarchy queries.
     * Will be exposed to JavaScript runtime.
     */
    fun getLayoutInspector(): LayoutInspector? {
        return layoutInspector
    }

    /**
     * Initialize automation with accessibility bridge.
     * Creates a background thread for gesture callbacks to avoid blocking the main thread.
     */
    fun initAutomation(bridge: AccessibilityBridge) {
        this.accessibilityBridge = bridge
        
        // Create a background thread for gesture callbacks
        // This avoids deadlock when calling gestures from the main thread
        if (gestureHandlerThread == null) {
            gestureHandlerThread = HandlerThread("GestureHandler").apply {
                start()
            }
            gestureHandler = Handler(gestureHandlerThread!!.looper)
        }
        
        this.automator = SimpleActionAutomator(bridge) {
            gestureHandler!!
        }
    }

    /**
     * Initialize layout inspector with accessibility bridge.
     */
    fun initLayoutInspector(bridge: AccessibilityBridge) {
        this.layoutInspector = LayoutInspector(context, bridge)
    }
    
    /**
     * Clean up resources.
     */
    fun destroy() {
        gestureHandlerThread?.quitSafely()
        gestureHandlerThread = null
        gestureHandler = null
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

