package com.autocljs.accessibility

import android.content.Context
import com.autocljs.util.UiHandler

/**
 * Concrete implementation of AccessibilityBridge for Phase 2.
 */
class AccessibilityBridgeImpl(
    context: Context,
    config: AccessibilityConfig,
    uiHandler: UiHandler
) : AccessibilityBridge(context, config, uiHandler) {

    private val infoProvider = ActivityInfoProvider()
    private val notificationObserver = AccessibilityNotificationObserver()

    override fun ensureServiceEnabled() {
        // For Phase 2, we'll just check if service is available
        // In a full implementation, this would check settings and prompt user
        if (getService() == null) {
            throw IllegalStateException("Accessibility service not enabled. Please enable it in Android settings.")
        }
    }

    override fun waitForServiceEnabled() {
        // Wait for service to be enabled
        val timeout = 5000L // 5 seconds
        val startTime = System.currentTimeMillis()
        while (getService() == null && (System.currentTimeMillis() - startTime) < timeout) {
            Thread.sleep(100)
        }
        if (getService() == null) {
            throw IllegalStateException("Accessibility service not enabled within timeout")
        }
    }

    override fun getService(): AccessibilityService? {
        return AccessibilityService.instance
    }

    override fun getInfoProvider(): ActivityInfoProvider {
        return infoProvider
    }

    override fun getNotificationObserver(): AccessibilityNotificationObserver {
        return notificationObserver
    }
}

