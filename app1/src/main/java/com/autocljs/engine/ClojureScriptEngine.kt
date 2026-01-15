package com.autocljs.engine

import android.content.Context
import android.util.Log
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.runtime.api.JsConsole
import com.autocljs.script.ScriptSource
import com.autocljs.script.StringScriptSource
import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/**
 * JavaScript engine implementation using Rhino.
 * 
 * Note: This engine executes compiled JavaScript code.
 * ClojureScript is compiled to JavaScript before reaching this library.
 * 
 * Phase 1: Basic Rhino integration to execute JavaScript code.
 */
class ClojureScriptEngine(private val context: Context) : ScriptEngine.AbstractScriptEngine<ScriptSource>() {
    
    private var runtime: ScriptRuntime? = null
    private var isInitialized = false
    
    // Rhino context and scope
    private var rhinoContext: Context? = null
    private var scope: Scriptable? = null
    
    companion object {
        private const val LOG_TAG = "ClojureScriptEngine"
    }
    
    override fun init() {
        if (!isInitialized) {
            runtime = ScriptRuntime(context)
            
            // Initialize Rhino context and scope
            initializeRhino()
            
            isInitialized = true
            Log.d(LOG_TAG, "Engine initialized with Rhino")
        }
    }
    
    /**
     * Initialize Rhino JavaScript engine.
     * Creates a Context and global scope for JavaScript execution.
     */
    private fun initializeRhino() {
        try {
            // Enter Rhino context
            rhinoContext = Context.enter()
            
            // Configure context
            rhinoContext?.let { ctx ->
                ctx.optimizationLevel = -1  // Interpreted mode (better compatibility)
                ctx.languageVersion = Context.VERSION_ES6
            }
            
            // Create global scope
            scope = ImporterTopLevel().apply {
                rhinoContext?.let { ctx ->
                    initStandardObjects(ctx, false)
                }
            }
            
            // Expose Console API to JavaScript
            exposeConsole()
            
            Log.d(LOG_TAG, "Rhino context and scope initialized")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to initialize Rhino", e)
            throw RuntimeException("Failed to initialize Rhino JavaScript engine", e)
        }
    }
    
    /**
     * Expose Console API to JavaScript.
     * Makes console.log(), console.error(), etc. available in JavaScript code.
     */
    private fun exposeConsole() {
        val ctx = rhinoContext ?: throw IllegalStateException("Context not initialized")
        val scp = scope ?: throw IllegalStateException("Scope not initialized")
        val rt = runtime ?: throw IllegalStateException("Runtime not initialized")
        
        try {
            // Create JavaScript console wrapper
            val jsConsole = JsConsole(rt.console)
            
            // Set parent scope for the console object
            jsConsole.parentScope = scp
            
            // Expose console to JavaScript global scope
            ScriptableObject.putProperty(scp, "console", jsConsole)
            
            Log.d(LOG_TAG, "Console API exposed to JavaScript")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to expose console API", e)
            throw RuntimeException("Failed to expose console API", e)
        }
    }
    
    override fun put(name: String, value: Any?) {
        val ctx = rhinoContext ?: throw IllegalStateException("Engine not initialized. Call init() first.")
        val scp = scope ?: throw IllegalStateException("Scope not initialized.")
        
        try {
            ScriptableObject.putProperty(scp, name, Context.javaToJS(value, scp))
            Log.d(LOG_TAG, "put($name, $value)")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to put property: $name", e)
            throw RuntimeException("Failed to put property: $name", e)
        }
    }
    
    override fun execute(scriptSource: ScriptSource): Any? {
        if (!isInitialized) {
            init()
        }
        
        if (scriptSource is StringScriptSource) {
            return executeJavaScript(scriptSource.script, scriptSource.name)
        }
        
        throw IllegalArgumentException("Unsupported script source type: ${scriptSource::class.java.name}")
    }
    
    /**
     * Execute JavaScript code using Rhino.
     * 
     * @param script JavaScript code to execute
     * @param sourceName Name of the script source (for error reporting)
     * @return Result of script execution, or null
     */
    private fun executeJavaScript(script: String, sourceName: String): Any? {
        val ctx = rhinoContext ?: throw IllegalStateException("Engine not initialized. Call init() first.")
        val scp = scope ?: throw IllegalStateException("Scope not initialized.")
        
        try {
            Log.d(LOG_TAG, "Executing JavaScript: $sourceName")
            Log.d(LOG_TAG, "Script length: ${script.length} characters")
            
            // Execute JavaScript code
            val result = ctx.evaluateString(scp, script, sourceName, 1, null)
            
            Log.d(LOG_TAG, "JavaScript execution completed: $sourceName")
            return result
        } catch (e: org.mozilla.javascript.RhinoException) {
            Log.e(LOG_TAG, "Rhino error executing script: $sourceName", e)
            Log.e(LOG_TAG, "Error message: ${e.message}")
            Log.e(LOG_TAG, "Line number: ${e.lineNumber()}, Column: ${e.columnNumber()}")
            Log.e(LOG_TAG, "Source: ${e.lineSource()}")
            throw RuntimeException("JavaScript execution error in $sourceName: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error executing script: $sourceName", e)
            throw RuntimeException("Error executing script: $sourceName", e)
        }
    }
    
    override fun forceStop() {
        Log.d(LOG_TAG, "forceStop() called")
        // Rhino doesn't have a built-in way to stop execution
        // We can interrupt the thread if needed, but for now just log
        // Thread interruption will be handled at a higher level if needed
    }
    
    override fun destroy() {
        super.destroy()
        
        // Exit Rhino context
        try {
            rhinoContext?.let {
                Context.exit()
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error exiting Rhino context", e)
        }
        
        rhinoContext = null
        scope = null
        runtime = null
        isInitialized = false
        Log.d(LOG_TAG, "Engine destroyed")
    }
}

