package com.autocljs.runtime.api

import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/**
 * JavaScript Console wrapper for Rhino.
 * 
 * Exposes console.log(), console.error(), console.warn(), console.info() to JavaScript.
 * Delegates to ScriptRuntime's Console implementation.
 */
class JsConsole(val console: Console) : ScriptableObject() {
    
    init {
        // Define function properties that will be callable from JavaScript
        defineFunctionProperties(
            arrayOf("log", "info", "warn", "error"),
            JsConsole::class.java,
            ScriptableObject.READONLY
        )
    }
    
    /**
     * Called by Rhino when JavaScript code calls console.log(...)
     */
    @JvmStatic
    fun log(context: Context, thisObj: Scriptable, args: Array<out Any?>?, funObj: BaseFunction): Any? {
        val message = formatArgs(args)
        // Access console from the instance - we need to get it from thisObj
        val jsConsole = thisObj as? JsConsole
        jsConsole?.console?.log(message)
        return Context.getUndefinedValue()
    }
    
    /**
     * Called by Rhino when JavaScript code calls console.info(...)
     */
    @JvmStatic
    fun info(context: Context, thisObj: Scriptable, args: Array<out Any?>?, funObj: BaseFunction): Any? {
        val message = formatArgs(args)
        val jsConsole = thisObj as? JsConsole
        jsConsole?.console?.info(message)
        return Context.getUndefinedValue()
    }
    
    /**
     * Called by Rhino when JavaScript code calls console.warn(...)
     */
    @JvmStatic
    fun warn(context: Context, thisObj: Scriptable, args: Array<out Any?>?, funObj: BaseFunction): Any? {
        val message = formatArgs(args)
        val jsConsole = thisObj as? JsConsole
        jsConsole?.console?.warn(message)
        return Context.getUndefinedValue()
    }
    
    /**
     * Called by Rhino when JavaScript code calls console.error(...)
     */
    @JvmStatic
    fun error(context: Context, thisObj: Scriptable, args: Array<out Any?>?, funObj: BaseFunction): Any? {
        val message = formatArgs(args)
        val jsConsole = thisObj as? JsConsole
        jsConsole?.console?.error(message)
        return Context.getUndefinedValue()
    }
    
    /**
     * Format JavaScript arguments into a string message.
     * Handles multiple arguments like console.log("Hello", "World", 123)
     */
    @JvmStatic
    private fun formatArgs(args: Array<out Any?>?): String {
        if (args == null || args.isEmpty()) {
            return ""
        }
        
        // Convert Rhino values to strings and join them
        return args.joinToString(" ") { arg ->
            when (arg) {
                null -> "null"
                is org.mozilla.javascript.Undefined -> "undefined"
                else -> {
                    // Convert Rhino objects to readable strings
                    try {
                        Context.toString(arg)
                    } catch (e: Exception) {
                        arg.toString()
                    }
                }
            }
        }
    }
    
    override fun getClassName(): String {
        return "Console"
    }
}

