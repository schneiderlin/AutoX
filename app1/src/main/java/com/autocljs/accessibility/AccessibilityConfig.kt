package com.autocljs.accessibility

/**
 * Configuration for accessibility service.
 * Minimal version for Phase 2.
 */
class AccessibilityConfig {
    private val whiteList = mutableListOf<String>()
    private var sealed = false

    fun whiteListContains(packageName: String): Boolean {
        return whiteList.contains(packageName)
    }

    fun addWhiteList(packageName: String) {
        if (sealed) {
            throw IllegalStateException("sealed")
        }
        whiteList.add(packageName)
    }

    fun seal() {
        sealed = true
    }
}

