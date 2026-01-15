package com.autocljs.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.autocljs.util.UiHandler

/**
 * Bridge to accessibility service.
 * Minimal version for Phase 2.
 */
abstract class AccessibilityBridge(
    private val context: Context,
    private val config: AccessibilityConfig,
    private val uiHandler: UiHandler
) {
    companion object {
        const val MODE_NORMAL = 0
        const val MODE_FAST = 1
    }

    private var mode: Int = MODE_NORMAL

    init {
        config.seal()
    }

    abstract fun ensureServiceEnabled()
    abstract fun waitForServiceEnabled()
    abstract fun getService(): AccessibilityService?
    abstract fun getInfoProvider(): ActivityInfoProvider
    abstract fun getNotificationObserver(): AccessibilityNotificationObserver

    fun post(r: Runnable) {
        uiHandler.post(r)
    }

    fun getRootInActiveWindow(): AccessibilityNodeInfo? {
        val service = getService() ?: return null
        return if ((mode and MODE_FAST) != 0) {
            service.fastRootInActiveWindow()
        } else {
            service.rootInActiveWindow
        }
    }

    fun windowRoots(): List<AccessibilityNodeInfo> {
        val service = getService() ?: return emptyList()
        val root = if ((mode and MODE_FAST) != 0) {
            service.fastRootInActiveWindow()
        } else {
            service.rootInActiveWindow
        }
        return if (root != null) listOf(root) else emptyList()
    }

    fun setMode(mode: Int) {
        this.mode = mode
    }

    fun getConfig(): AccessibilityConfig = config
}

