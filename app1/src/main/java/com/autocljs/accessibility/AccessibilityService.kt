package com.autocljs.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Minimal accessibility service for Phase 2.
 * Provides basic service lifecycle and root access.
 */
open class AccessibilityService : android.accessibilityservice.AccessibilityService() {

    private var fastRootInActiveWindow: AccessibilityNodeInfo? = null

    // Event executor for async operations
    private val eventExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        instance = this
        val type = event.eventType
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            type == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            val root = rootInActiveWindow
            if (root != null) {
                fastRootInActiveWindow = root
            }
        }
    }

    override fun onInterrupt() {
        // Minimal implementation
    }

    override fun getRootInActiveWindow(): AccessibilityNodeInfo? {
        return try {
            super.getRootInActiveWindow()
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: $instance")
        ENABLED = Job()
        instance = null
        eventExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onServiceConnected() {
        Log.d(TAG, "onServiceConnected: $serviceInfo")
        instance = this
        super.onServiceConnected()
        ENABLED.complete()
    }

    fun fastRootInActiveWindow(): AccessibilityNodeInfo? {
        return fastRootInActiveWindow
    }

    companion object {
        private const val TAG = "AccessibilityService"

        @Volatile
        private var ENABLED = Job()
        
        var instance: AccessibilityService? = null
            private set

        fun waitForEnabled(timeOut: Long): Boolean = runBlocking {
            if (instance != null) return@runBlocking true
            if (timeOut == -1L) {
                ENABLED.join()
                true
            } else {
                withTimeoutOrNull(timeOut) {
                    ENABLED.join()
                    true
                } != null
            }
        }
    }
}

