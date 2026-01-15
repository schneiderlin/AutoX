package com.autocljs.runtime.api

import com.autocljs.automation.SimpleActionAutomator
import com.autocljs.runtime.ScriptRuntime
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.reference.V8ValueObject

/**
 * JavaScript Auto wrapper for Node.js/V8 engine.
 * 
 * Exposes auto.click(), auto.longClick(), auto.press(), auto.swipe() to JavaScript.
 * Delegates to ScriptRuntime's SimpleActionAutomator.
 */
class NodeAuto(val runtime: ScriptRuntime) {
    private var v8ValueObject: V8ValueObject? = null
    
    /**
     * Install auto API in the global scope.
     */
    fun install(v8Runtime: V8Runtime, global: V8ValueObject) {
        v8Runtime.getExecutor(SCRIPT).execute<V8ValueObject>().let {
            v8ValueObject = it
            it.bind(this)
            global.set("auto", it)
        }
    }
    
    /**
     * Clean up auto API.
     */
    fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
        v8ValueObject?.use { it.unbind(this) }
        v8ValueObject = null
    }
    
    /**
     * Get the automator instance, throwing an error if not initialized.
     */
    private fun getAutomator(): SimpleActionAutomator {
        return runtime.getAutomator()
            ?: throw IllegalStateException("Automator not initialized. Call initAutomation() first.")
    }
    
    /**
     * Convert V8 value to Int.
     * Handles Number, String, and other numeric types.
     */
    private fun toInt(value: V8Value?): Int {
        if (value == null) {
            throw IllegalArgumentException("Expected number, got null")
        }
        
        // Use converter to get the Java object, then convert to Int
        val obj = value.v8Runtime.converter.toObject<Any?>(value)
        return when (obj) {
            is Number -> obj.toInt()
            is String -> obj.toIntOrNull() ?: throw IllegalArgumentException("Cannot convert '$obj' to integer")
            else -> throw IllegalArgumentException("Expected number, got ${obj?.javaClass?.simpleName ?: "null"}")
        }
    }
    
    /**
     * Called by V8 when JavaScript code calls auto.click(x, y)
     */
    @V8Function
    fun click(x: V8Value?, y: V8Value?): Boolean {
        val xInt = toInt(x)
        val yInt = toInt(y)
        return getAutomator().click(xInt, yInt)
    }
    
    /**
     * Called by V8 when JavaScript code calls auto.longClick(x, y)
     */
    @V8Function
    fun longClick(x: V8Value?, y: V8Value?): Boolean {
        val xInt = toInt(x)
        val yInt = toInt(y)
        return getAutomator().longClick(xInt, yInt)
    }
    
    /**
     * Called by V8 when JavaScript code calls auto.press(x, y, delay)
     */
    @V8Function
    fun press(x: V8Value?, y: V8Value?, delay: V8Value?): Boolean {
        val xInt = toInt(x)
        val yInt = toInt(y)
        val delayInt = toInt(delay)
        return getAutomator().press(xInt, yInt, delayInt)
    }
    
    /**
     * Called by V8 when JavaScript code calls auto.swipe(x1, y1, x2, y2, delay)
     */
    @V8Function
    fun swipe(x1: V8Value?, y1: V8Value?, x2: V8Value?, y2: V8Value?, delay: V8Value?): Boolean {
        val x1Int = toInt(x1)
        val y1Int = toInt(y1)
        val x2Int = toInt(x2)
        val y2Int = toInt(y2)
        val delayInt = toInt(delay)
        return getAutomator().swipe(x1Int, y1Int, x2Int, y2Int, delayInt)
    }
    
    companion object {
        /**
         * JavaScript code to create auto object.
         */
        private val SCRIPT = """
            (function () {
                const nativeAuto = {};
                return nativeAuto;
            })()
        """.trimIndent()
    }
}

