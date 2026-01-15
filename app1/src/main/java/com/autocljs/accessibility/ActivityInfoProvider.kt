package com.autocljs.accessibility

/**
 * Minimal activity info provider for Phase 2.
 * Provides basic package/activity tracking.
 */
class ActivityInfoProvider {
    @Volatile
    var latestPackage: String = ""

    @Volatile
    var latestActivity: String = ""

    var useUsageStats: Boolean = false
    var useShell: Boolean = false
}

