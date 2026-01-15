package com.autocljs.util

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * UI handler for posting runnables to the main thread.
 */
class UiHandler(context: Context) : Handler(Looper.getMainLooper()) {
    // Simple handler that extends Handler with main looper
    // Can add toast methods later if needed
}

