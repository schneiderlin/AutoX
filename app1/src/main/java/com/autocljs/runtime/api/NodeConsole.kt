package com.autocljs.runtime.api

import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.reference.V8ValueObject

/**
 * JavaScript Console wrapper for Node.js/V8 engine.
 * 
 * Exposes console.log(), console.error(), console.warn(), console.info() to JavaScript.
 * Delegates to ScriptRuntime's Console implementation.
 */
class NodeConsole(
    val console: Console
) {
    private var v8ValueObject: V8ValueObject? = null
    
    /**
     * Install console API in the global scope.
     */
    fun install(v8Runtime: V8Runtime, global: V8ValueObject) {
        v8Runtime.getExecutor(SCRIPT).execute<V8ValueObject>().let {
            v8ValueObject = it
            it.bind(this)
            global.set("console", it)
        }
    }
    
    /**
     * Clean up console API.
     */
    fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
        v8ValueObject?.use { it.unbind(this) }
        v8ValueObject = null
    }
    
    @V8Function
    fun log(vararg v8Value: V8Value?) {
        val message = concat(*v8Value)
        console.log(message)
    }
    
    @V8Function
    fun info(vararg v8Value: V8Value?) {
        val message = concat(*v8Value)
        console.info(message)
    }
    
    @V8Function
    fun warn(vararg v8Value: V8Value?) {
        val message = concat(*v8Value)
        console.warn(message)
    }
    
    @V8Function
    fun error(vararg v8Value: V8Value?) {
        val message = concat(*v8Value)
        console.error(message)
    }
    
    /**
     * Error method that accepts a String directly (for exception handling).
     */
    fun error(message: String) {
        console.error(message)
    }
    
    /**
     * Log method that accepts a String directly (for exception handling).
     */
    fun log(message: String) {
        console.log(message)
    }
    
    /**
     * Concatenate V8 values into a string message.
     * Handles multiple arguments like console.log("Hello", "World", 123)
     */
    private fun concat(vararg obj: Any?): String {
        return obj.joinToString(" ") { value ->
            when (value) {
                is V8Value -> {
                    try {
                        // Use converter to get readable string representation
                        value.v8Runtime.converter.toObject<Any?>(value)?.toString() ?: "[object]"
                    } catch (e: Exception) {
                        "[object]"
                    }
                }
                null -> "null"
                else -> value.toString()
            }
        }.trim()
    }
    
    companion object {
        /**
         * JavaScript code to create console object.
         */
        private val SCRIPT = """
            (function () {
                const nativeConsole = {};
                return nativeConsole;
            })()
        """.trimIndent()
    }
}

