package com.autocljs.runtime.api

import android.util.Log

/**
 * Simple console implementation that outputs to Android Log.
 */
class SimpleConsole(private val tag: String = "AutoCLJS") : Console {
    
    override fun log(data: Any?, vararg options: Any?) {
        Log.d(tag, formatMessage(data, *options))
    }

    override fun info(data: Any?, vararg options: Any?) {
        Log.i(tag, formatMessage(data, *options))
    }

    override fun warn(data: Any?, vararg options: Any?) {
        Log.w(tag, formatMessage(data, *options))
    }

    override fun error(data: Any?, vararg options: Any?) {
        Log.e(tag, formatMessage(data, *options))
    }

    private fun formatMessage(data: Any?, vararg options: Any?): String {
        val sb = StringBuilder()
        if (data != null) {
            sb.append(data.toString())
        }
        if (options.isNotEmpty()) {
            for (option in options) {
                sb.append(" ").append(option?.toString() ?: "null")
            }
        }
        return sb.toString()
    }
}

