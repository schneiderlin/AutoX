package com.autocljs.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Screen metrics for coordinate scaling.
 */
class ScreenMetrics {
    private var designWidth: Int = 0
    private var designHeight: Int = 0

    constructor(designWidth: Int, designHeight: Int) {
        this.designWidth = designWidth
        this.designHeight = designHeight
    }

    constructor()

    fun setDesignWidth(designWidth: Int) {
        this.designWidth = designWidth
    }

    fun setDesignHeight(designHeight: Int) {
        this.designHeight = designHeight
    }

    fun setScreenMetrics(width: Int, height: Int) {
        this.designWidth = width
        this.designHeight = height
    }

    fun scaleX(x: Int): Int {
        if (designWidth == 0 || !initialized) return x
        return x * deviceScreenWidth / designWidth
    }

    fun scaleY(y: Int): Int {
        if (designHeight == 0 || !initialized) return y
        return y * deviceScreenHeight / designHeight
    }

    companion object {
        @JvmStatic
        var deviceScreenHeight: Int = 0
            private set

        @JvmStatic
        var deviceScreenWidth: Int = 0
            private set

        private var initialized = false

        fun initIfNeeded(context: Context) {
            if (initialized && deviceScreenHeight != 0) return
            val metrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getRealMetrics(metrics)
            deviceScreenHeight = metrics.heightPixels
            deviceScreenWidth = metrics.widthPixels
            initialized = true
        }

        @JvmStatic
        fun scaleX(x: Int, width: Int = 0): Int {
            if (width == 0 || !initialized) return x
            return x * deviceScreenWidth / width
        }

        @JvmStatic
        fun scaleY(y: Int, height: Int = 0): Int {
            if (height == 0 || !initialized) return y
            return y * deviceScreenHeight / height
        }
    }
}

