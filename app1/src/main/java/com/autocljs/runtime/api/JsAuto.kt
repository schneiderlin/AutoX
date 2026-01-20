package com.autocljs.runtime.api

import com.autocljs.automation.SimpleActionAutomator
import com.autocljs.layout.NodeInfo
import com.autocljs.runtime.ScriptRuntime
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/**
 * JavaScript Auto wrapper for Rhino.
 * 
 * Exposes auto.click(), auto.longClick(), auto.press(), auto.swipe() to JavaScript.
 * Delegates to ScriptRuntime's SimpleActionAutomator.
 */
class JsAuto(val runtime: ScriptRuntime) : ScriptableObject() {
    
    init {
        // Define function properties that will be callable from JavaScript
        defineFunctionProperties(
            arrayOf("click", "longClick", "press", "swipe", "clickNode"),
            JsAuto::class.java,
            ScriptableObject.READONLY
        )
    }
    
    /**
     * Get the automator instance, throwing an error if not initialized.
     */
    private fun getAutomator(): SimpleActionAutomator {
        return runtime.getAutomator()
            ?: throw IllegalStateException("Automator not initialized. Call initAutomation() first.")
    }
    
    override fun getClassName(): String {
        return "Auto"
    }
    
    companion object {
        /**
         * Convert JavaScript number to Int.
         * Handles Double, Int, and other numeric types.
         */
        private fun toInt(value: Any?): Int {
            return when (value) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull() ?: throw IllegalArgumentException("Cannot convert '$value' to integer")
                null -> throw IllegalArgumentException("Expected number, got null")
                else -> throw IllegalArgumentException("Expected number, got ${value::class.simpleName}")
            }
        }
        
        /**
         * Called by Rhino when JavaScript code calls auto.click(x, y)
         */
        @JvmStatic
        fun click(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsAuto = thisObj as? JsAuto
                ?: throw IllegalStateException("Invalid thisObj for auto.click")
            
            if (args.size < 2) {
                throw IllegalArgumentException("auto.click() requires 2 arguments: x, y")
            }
            
            val x = toInt(args[0])
            val y = toInt(args[1])
            
            val result = jsAuto.getAutomator().click(x, y)
            return result
        }
        
        /**
         * Called by Rhino when JavaScript code calls auto.longClick(x, y)
         */
        @JvmStatic
        fun longClick(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsAuto = thisObj as? JsAuto
                ?: throw IllegalStateException("Invalid thisObj for auto.longClick")
            
            if (args.size < 2) {
                throw IllegalArgumentException("auto.longClick() requires 2 arguments: x, y")
            }
            
            val x = toInt(args[0])
            val y = toInt(args[1])
            
            val result = jsAuto.getAutomator().longClick(x, y)
            return result
        }
        
        /**
         * Called by Rhino when JavaScript code calls auto.press(x, y, delay)
         */
        @JvmStatic
        fun press(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsAuto = thisObj as? JsAuto
                ?: throw IllegalStateException("Invalid thisObj for auto.press")
            
            if (args.size < 3) {
                throw IllegalArgumentException("auto.press() requires 3 arguments: x, y, delay")
            }
            
            val x = toInt(args[0])
            val y = toInt(args[1])
            val delay = toInt(args[2])
            
            val result = jsAuto.getAutomator().press(x, y, delay)
            return result
        }
        
        /**
         * Called by Rhino when JavaScript code calls auto.swipe(x1, y1, x2, y2, delay)
         */
        @JvmStatic
        fun swipe(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsAuto = thisObj as? JsAuto
                ?: throw IllegalStateException("Invalid thisObj for auto.swipe")
            
            if (args.size < 5) {
                throw IllegalArgumentException("auto.swipe() requires 5 arguments: x1, y1, x2, y2, delay")
            }
            
            val x1 = toInt(args[0])
            val y1 = toInt(args[1])
            val x2 = toInt(args[2])
            val y2 = toInt(args[3])
            val delay = toInt(args[4])
            
            val result = jsAuto.getAutomator().swipe(x1, y1, x2, y2, delay)
            return result
        }

        /**
         * Called by Rhino when JavaScript code calls auto.clickNode(node)
         */
        @JvmStatic
        fun clickNode(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsAuto = thisObj as? JsAuto
                ?: throw IllegalStateException("Invalid thisObj for auto.clickNode")

            if (args.isEmpty()) {
                throw IllegalArgumentException("auto.clickNode() requires at least 1 argument: node")
            }

            val node = args[0] as? NodeInfo
                ?: throw IllegalArgumentException("auto.clickNode() first argument must be a NodeInfo object")

            val result = jsAuto.getAutomator().clickNode(node)
            return result
        }
    }
}

