package com.autocljs.automation

import android.accessibilityservice.GestureDescription
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import com.autocljs.accessibility.AccessibilityBridge
import com.autocljs.util.ScreenMetrics

/**
 * Simple action automator for coordinate-based actions.
 * Minimal version for Phase 2.
 */
class SimpleActionAutomator(
    private val accessibilityBridge: AccessibilityBridge,
    private val handlerProvider: () -> Handler
) {
    private lateinit var globalActionAutomator: GlobalActionAutomator
    private var screenMetrics: ScreenMetrics? = null

    fun click(x: Int, y: Int): Boolean {
        prepareForGesture()
        return globalActionAutomator.click(x, y)
    }

    fun longClick(x: Int, y: Int): Boolean {
        prepareForGesture()
        return globalActionAutomator.longClick(x, y)
    }

    fun press(x: Int, y: Int, delay: Int): Boolean {
        prepareForGesture()
        return globalActionAutomator.press(x, y, delay)
    }

    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, delay: Int): Boolean {
        prepareForGesture()
        return globalActionAutomator.swipe(x1, y1, x2, y2, delay.toLong())
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun gesture(start: Long, duration: Long, vararg points: IntArray): Boolean {
        prepareForGesture()
        return globalActionAutomator.gesture(start, duration, *points)
    }

    fun setScreenMetrics(metrics: ScreenMetrics) {
        this.screenMetrics = metrics
    }

    private fun prepareForGesture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            throw UnsupportedOperationException("Gestures require API 24+")
        }
        if (!::globalActionAutomator.isInitialized) {
            globalActionAutomator = GlobalActionAutomator(handlerProvider()) {
                ensureAccessibilityServiceEnabled()
                accessibilityBridge.getService()!!
            }
        }
        globalActionAutomator.setScreenMetrics(screenMetrics)
    }

    private fun ensureAccessibilityServiceEnabled() {
        accessibilityBridge.ensureServiceEnabled()
    }
}

