package com.autocljs.util

/**
 * Simple volatile box for thread-safe value storage.
 */
class VolatileBox<T> {
    @Volatile
    private var value: T? = null

    constructor()

    constructor(value: T) {
        this.value = value
    }

    fun get(): T? = value

    fun set(value: T?) {
        this.value = value
    }

    fun isNull(): Boolean = value == null

    fun notNull(): Boolean = value != null

    @Synchronized
    fun setAndNotify(value: T?) {
        this.value = value
        notify()
    }

    @Synchronized
    fun blockedGet(): T? {
        while (value == null) {
            try {
                wait()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
        return value
    }
}

