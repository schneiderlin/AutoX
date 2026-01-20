package com.autocljs.test

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.stardust.enhancedfloaty.FloatyService

/**
 * Simple floating window service that displays a single button.
 * When clicked, it prints "Hello World" to logcat and shows a toast.
 */
class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var floatingButton: Button

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Start the EnhancedFloaty library service
        startService(Intent(this, FloatyService::class.java))

        // Create and add the floating button
        addFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the floating view
        floatingView?.let {
            windowManager.removeView(it)
        }
        floatingView = null
    }

    private fun addFloatingButton() {
        // Inflate the floating button layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button, null)

        // Configure the button
        floatingButton = floatingView!!.findViewById(R.id.floatingButton)
        floatingButton.setOnClickListener {
            onFloatingButtonClick()
        }

        // Set up drag gesture
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingView!!.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                return when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = (floatingView!!.layoutParams as WindowManager.LayoutParams).x
                        initialY = (floatingView!!.layoutParams as WindowManager.LayoutParams).y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val params = floatingView!!.layoutParams as WindowManager.LayoutParams
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        true
                    }
                    else -> false
                }
            }
        })

        // Configure layout parameters for the floating window
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200

        // Add the view to the window manager
        windowManager.addView(floatingView, params)
    }

    private fun onFloatingButtonClick() {
        val message = "Hello World!"
        android.util.Log.d("FloatingWindow", message)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
