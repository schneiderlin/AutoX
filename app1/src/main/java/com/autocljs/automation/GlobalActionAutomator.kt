package com.autocljs.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewConfiguration
import androidx.annotation.RequiresApi
import com.autocljs.util.ScreenMetrics
import com.autocljs.util.VolatileBox
import com.autocljs.util.VolatileDispose

/**
 * Global action automator for performing gestures.
 * Minimal version for Phase 2 - coordinate-based clicks.
 */
class GlobalActionAutomator(
    private val handler: Handler?,
    private val serviceProvider: () -> AccessibilityService
) {
    private val service: AccessibilityService
        get() = serviceProvider()

    private var screenMetrics: ScreenMetrics? = null

    fun setScreenMetrics(screenMetrics: ScreenMetrics?) {
        this.screenMetrics = screenMetrics
    }

    fun click(x: Int, y: Int): Boolean {
        return press(x, y, ViewConfiguration.getTapTimeout() + 50)
    }

    fun press(x: Int, y: Int, delay: Int): Boolean {
        return gesture(0, delay.toLong(), intArrayOf(x, y))
    }

    fun longClick(x: Int, y: Int): Boolean {
        return gesture(0, (ViewConfiguration.getLongPressTimeout() + 200).toLong(), intArrayOf(x, y))
    }

    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, delay: Long): Boolean {
        return gesture(0, delay, intArrayOf(x1, y1), intArrayOf(x2, y2))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun gesture(start: Long, duration: Long, vararg points: IntArray): Boolean {
        val path = pointsToPath(points)
        return gestures(GestureDescription.StrokeDescription(path, start, duration))
    }

    private fun pointsToPath(points: Array<out IntArray>): Path {
        val path = Path()
        path.moveTo(scaleX(points[0][0]).toFloat(), scaleY(points[0][1]).toFloat())
        for (i in 1 until points.size) {
            val point = points[i]
            path.lineTo(scaleX(point[0]).toFloat(), scaleY(point[1]).toFloat())
        }
        return path
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun gestures(vararg strokes: GestureDescription.StrokeDescription): Boolean {
        val builder = GestureDescription.Builder()
        for (stroke in strokes) {
            builder.addStroke(stroke)
        }
        val handler = this.handler
        return if (handler == null) {
            gesturesWithoutHandler(builder.build())
        } else {
            gesturesWithHandler(handler, builder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun gesturesWithHandler(handler: Handler, description: GestureDescription): Boolean {
        val result = VolatileDispose<Boolean>()
        service.dispatchGesture(description, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                result.setAndNotify(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                result.setAndNotify(false)
            }
        }, handler)
        return result.blockedGet()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun gesturesWithoutHandler(description: GestureDescription): Boolean {
        prepareLooperIfNeeded()
        val result = VolatileBox(false)
        val handler = Looper.myLooper()?.let { Handler(it) }
        service.dispatchGesture(description, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                result.set(true)
                quitLoop()
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                result.set(false)
                quitLoop()
            }
        }, handler)
        Looper.loop()
        return result.get() ?: false
    }

    private fun quitLoop() {
        val looper = Looper.myLooper()
        looper?.quit()
    }

    private fun prepareLooperIfNeeded() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
    }

    private fun scaleX(x: Int): Int {
        return screenMetrics?.scaleX(x) ?: ScreenMetrics.scaleX(x)
    }

    private fun scaleY(y: Int): Int {
        return screenMetrics?.scaleY(y) ?: ScreenMetrics.scaleY(y)
    }
}

