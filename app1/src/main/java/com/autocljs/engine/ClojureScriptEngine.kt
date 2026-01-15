package com.autocljs.engine

import android.content.Context
import android.util.Log
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.script.ScriptSource
import com.autocljs.script.StringScriptSource

/**
 * JavaScript engine implementation.
 * 
 * Note: This engine executes compiled JavaScript code.
 * ClojureScript is compiled to JavaScript before reaching this library.
 * 
 * For Phase 1, this is a placeholder that will be fully implemented
 * when JavaScript engine integration is added (e.g., Rhino, V8, or similar).
 * 
 * Currently, it just logs the script execution for testing.
 */
class ClojureScriptEngine(private val context: Context) : ScriptEngine.AbstractScriptEngine<ScriptSource>() {
    
    private var runtime: ScriptRuntime? = null
    private var isInitialized = false
    
    override fun init() {
        if (!isInitialized) {
            runtime = ScriptRuntime(context)
            isInitialized = true
            Log.d("ClojureScriptEngine", "Engine initialized")
        }
    }
    
    override fun put(name: String, value: Any?) {
        // TODO: Store variables in JavaScript runtime
        Log.d("ClojureScriptEngine", "put($name, $value)")
    }
    
    override fun execute(scriptSource: ScriptSource): Any? {
        if (!isInitialized) {
            init()
        }
        
        if (scriptSource is StringScriptSource) {
            // For Phase 1, just log that we would execute the script
            runtime?.console?.log("Would execute JavaScript: ${scriptSource.name}")
            runtime?.console?.log("Script content (first 100 chars): ${scriptSource.script.take(100)}")
            
            // TODO: Actually execute JavaScript code
            // This will be implemented when JavaScript engine is integrated (Rhino, V8, etc.)
            // The script is already compiled JavaScript from ClojureScript
            
            return null
        }
        
        throw IllegalArgumentException("Unsupported script source type: ${scriptSource::class.java.name}")
    }
    
    override fun forceStop() {
        Log.d("ClojureScriptEngine", "forceStop() called")
        // TODO: Stop JavaScript execution
    }
    
    override fun destroy() {
        super.destroy()
        runtime = null
        isInitialized = false
        Log.d("ClojureScriptEngine", "Engine destroyed")
    }
}

