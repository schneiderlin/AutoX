package com.autocljs.runtime.api

/**
 * Console interface for outputting messages.
 * Simplified version for Phase 1 - only essential logging methods.
 */
interface Console {
    fun log(data: Any?, vararg options: Any?)
    fun info(data: Any?, vararg options: Any?)
    fun warn(data: Any?, vararg options: Any?)
    fun error(data: Any?, vararg options: Any?)
}

