package com.autocljs

import android.content.Context
import android.util.Log
import com.autocljs.engine.ClojureScriptEngine
import com.autocljs.engine.NodeScriptEngine
import com.autocljs.engine.ScriptEngine
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.runtime.api.Console
import com.autocljs.runtime.api.SimpleConsole
import com.autocljs.script.ScriptSource

/**
 * Script engine service that supports both Rhino and Node.js engines.
 * Selects the appropriate engine based on ScriptSource.engineName.
 */
class ScriptEngineService private constructor(
    private val context: Context,
    val globalConsole: Console = SimpleConsole("AutoCLJS"),
    private val sharedRuntime: ScriptRuntime? = null
) {
    
    // Rhino engine for .js files (backward compatibility)
    private val rhinoEngine: ScriptEngine<ScriptSource> by lazy {
        ClojureScriptEngine(context, sharedRuntime).apply {
            id = 0
            init()
        }
    }
    
    // Node.js engine for .mjs files (ES modules)
    private val nodeEngine: ScriptEngine<ScriptSource> by lazy {
        NodeScriptEngine(context, sharedRuntime).apply {
            id = 1
            init()
        }
    }
    
    // Map of active engines by ID
    private val activeEngines = mutableMapOf<Int, ScriptEngine<ScriptSource>>()
    
    init {
        ScriptRuntime.setApplicationContext(context)
        Log.d("ScriptEngineService", "Service initialized with multi-engine support")
    }
    
    /**
     * Get the appropriate engine for a script source.
     */
    private fun getEngine(source: ScriptSource): ScriptEngine<ScriptSource> {
        return when (source.engineName) {
            NodeScriptEngine.ID -> {
                activeEngines[nodeEngine.id] = nodeEngine
                nodeEngine
            }
            else -> {
                // Default to Rhino for backward compatibility
                activeEngines[rhinoEngine.id] = rhinoEngine
                rhinoEngine
            }
        }
    }
    
    /**
     * Execute a script source using the appropriate engine.
     */
    fun execute(source: ScriptSource): ScriptExecution {
        val engine = getEngine(source)
        return try {
            val result = engine.execute(source)
            ScriptExecution(engine.id, source, result, null)
        } catch (e: Throwable) {
            Log.e("ScriptEngineService", "Error executing script: ${source.name}", e)
            engine.uncaughtException(e)
            ScriptExecution(engine.id, source, null, e)
        }
    }
    
    /**
     * Stop all running scripts.
     */
    fun stopAll(): Int {
        var stopped = 0
        activeEngines.values.forEach { engine ->
            if (!engine.isDestroyed) {
                engine.forceStop()
                stopped++
            }
        }
        return stopped
    }
    
    /**
     * Builder for ScriptEngineService.
     */
    class Builder(private val context: Context) {
        private var console: Console? = null
        private var runtime: ScriptRuntime? = null
        
        fun setConsole(console: Console): Builder {
            this.console = console
            return this
        }
        
        fun setRuntime(runtime: ScriptRuntime): Builder {
            this.runtime = runtime
            return this
        }
        
        fun build(): ScriptEngineService {
            return ScriptEngineService(
                context,
                console ?: SimpleConsole("AutoCLJS"),
                runtime
            )
        }
    }
}

/**
 * Simple script execution result.
 */
data class ScriptExecution(
    val id: Int,
    val source: ScriptSource,
    val result: Any?,
    val exception: Throwable?
)

