package com.autocljs.util

/**
 * Volatile dispose for blocking get operations.
 */
class VolatileDispose<T> {
    @Volatile
    private var value: T? = null

    @Synchronized
    fun blockedGet(): T {
        if (value != null) {
            return value!!
        }
        try {
            wait()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        return value!!
    }

    @Synchronized
    fun setAndNotify(value: T) {
        this.value = value
        notify()
    }
}

