package com.autocljs.engine

import android.content.Context
import android.util.Log
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.runtime.api.JsAuto
import com.autocljs.runtime.api.JsConsole
import com.autocljs.runtime.api.JsLayout
import com.autocljs.script.ScriptSource
import com.autocljs.script.StringScriptSource
import org.mozilla.javascript.Context as RhinoContext
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
 * Phase 2: Console API exposed (console.log, console.error, etc.)
 * Phase 3: Auto API exposed (auto.click, auto.longClick, etc.)
 */
class ClojureScriptEngine(
    private val context: Context,
    private val sharedRuntime: ScriptRuntime? = null
) : ScriptEngine.AbstractScriptEngine<ScriptSource>() {
    
    private var runtime: ScriptRuntime? = null
    private var isInitialized = false
    
    // Rhino scope (can be shared across threads)
    private var scope: Scriptable? = null
    
    companion object {
        private const val LOG_TAG = "ClojureScriptEngine"
    }
    
    @Synchronized
    override fun init() {
        if (!isInitialized) {
            // Use shared runtime if provided, otherwise create a new one
            runtime = sharedRuntime ?: ScriptRuntime(context)
            
            // Initialize Rhino scope
            initializeRhino()
            
            isInitialized = true
            Log.d(LOG_TAG, "Engine initialized with Rhino")
        }
    }
    
    /**
     * Initialize Rhino JavaScript engine.
     * Creates a global scope for JavaScript execution.
     * Note: Rhino Context is thread-local, so we enter/exit on each execution.
     */
    private fun initializeRhino() {
        // Enter context temporarily for initialization
        val ctx = RhinoContext.enter()
        try {
            // Configure context
            ctx.optimizationLevel = -1  // Interpreted mode (better compatibility)
            ctx.languageVersion = RhinoContext.VERSION_ES6
            
            // Create global scope
            scope = ImporterTopLevel().apply {
                initStandardObjects(ctx, false)
            }
            
            // Expose Console API to JavaScript
            exposeConsole(ctx)

            // Expose Auto API to JavaScript
            exposeAuto(ctx)

            // Expose Layout API to JavaScript
            exposeLayout(ctx)
            
            Log.d(LOG_TAG, "Rhino scope initialized")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to initialize Rhino", e)
            throw RuntimeException("Failed to initialize Rhino JavaScript engine", e)
        } finally {
            RhinoContext.exit()
        }
    }
    
    /**
     * Expose Console API to JavaScript.
     * Makes console.log(), console.error(), etc. available in JavaScript code.
     */
    private fun exposeConsole(ctx: RhinoContext) {
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
    
    /**
     * Expose Auto API to JavaScript.
     * Makes auto.click(), auto.longClick(), etc. available in JavaScript code.
     * 
     * Note: The automator must be initialized via runtime.initAutomation() before
     * JavaScript code can use the auto API. This is typically done in TestActivity
     * or similar before executing scripts.
     */
    private fun exposeAuto(ctx: RhinoContext) {
        val scp = scope ?: throw IllegalStateException("Scope not initialized")
        val rt = runtime ?: throw IllegalStateException("Runtime not initialized")
        
        try {
            // Create JavaScript auto wrapper
            val jsAuto = JsAuto(rt)
            
            // Set parent scope for the auto object
            jsAuto.parentScope = scp
            
            // Expose auto to JavaScript global scope
            ScriptableObject.putProperty(scp, "auto", jsAuto)
            
            Log.d(LOG_TAG, "Auto API exposed to JavaScript")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to expose auto API", e)
            throw RuntimeException("Failed to expose auto API", e)
        }
    }

    /**
     * Expose Layout API to JavaScript.
     * Makes layout.findOne(), layout.findAll(), etc. available in JavaScript code.
     *
     * Note: The layout inspector must be initialized via runtime.initLayoutInspector() before
     * JavaScript code can use the layout API. This is typically done in TestActivity
     * or similar before executing scripts.
     */
    private fun exposeLayout(ctx: RhinoContext) {
        val scp = scope ?: throw IllegalStateException("Scope not initialized")
        val rt = runtime ?: throw IllegalStateException("Runtime not initialized")

        try {
            // Create JavaScript layout wrapper
            val jsLayout = JsLayout(rt)

            // Set parent scope for the layout object
            jsLayout.parentScope = scp

            // Expose layout to JavaScript global scope
            ScriptableObject.putProperty(scp, "layout", jsLayout)

            Log.d(LOG_TAG, "Layout API exposed to JavaScript")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to expose layout API", e)
            throw RuntimeException("Failed to expose layout API", e)
        }
    }

    override fun put(name: String, value: Any?) {
        if (!isInitialized) {
            throw IllegalStateException("Engine not initialized. Call init() first.")
        }
        val scp = scope ?: throw IllegalStateException("Scope not initialized.")
        
        val ctx = RhinoContext.enter()
        try {
            ctx.optimizationLevel = -1
            ctx.languageVersion = RhinoContext.VERSION_ES6
            ScriptableObject.putProperty(scp, name, RhinoContext.javaToJS(value, scp))
            Log.d(LOG_TAG, "put($name, $value)")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to put property: $name", e)
            throw RuntimeException("Failed to put property: $name", e)
        } finally {
            RhinoContext.exit()
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
     * Note: Rhino Context is thread-local, so we enter/exit on each execution
     * to support multi-threaded script execution.
     * 
     * @param script JavaScript code to execute
     * @param sourceName Name of the script source (for error reporting)
     * @return Result of script execution, or null
     */
    private fun executeJavaScript(script: String, sourceName: String): Any? {
        val scp = scope ?: throw IllegalStateException("Scope not initialized.")
        
        // Enter Rhino context for this thread
        val ctx = RhinoContext.enter()
        try {
            // Configure context (same settings as initialization)
            ctx.optimizationLevel = -1  // Interpreted mode (better compatibility)
            ctx.languageVersion = RhinoContext.VERSION_ES6
            
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
        } finally {
            // Always exit the context when done
            RhinoContext.exit()
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
        
        scope = null
        runtime = null
        isInitialized = false
        Log.d(LOG_TAG, "Engine destroyed")
    }
}

