package com.autocljs

import android.content.Context
import android.util.Log
import com.autocljs.engine.ClojureScriptEngine
import com.autocljs.engine.ScriptEngine
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.runtime.api.Console
import com.autocljs.runtime.api.SimpleConsole
import com.autocljs.script.ScriptSource

/**
 * Simplified script engine service for Phase 1.
 * Uses a singleton engine for script execution.
 */
class ScriptEngineService private constructor(
    private val context: Context,
    val globalConsole: Console = SimpleConsole("AutoCLJS"),
    private val sharedRuntime: ScriptRuntime? = null
) {
    
    private val engine: ScriptEngine<ScriptSource> by lazy {
        ClojureScriptEngine(context, sharedRuntime).apply {
            id = 0
            init()
        }
    }
    
    init {
        ScriptRuntime.setApplicationContext(context)
        Log.d("ScriptEngineService", "Service initialized")
    }
    
    /**
     * Execute a script source using the singleton engine.
     */
    fun execute(source: ScriptSource): ScriptExecution {
        return try {
            val result = engine.execute(source)
            ScriptExecution(engine.id, source, result, null)
        } catch (e: Throwable) {
            Log.e("ScriptEngineService", "Error executing script", e)
            engine.uncaughtException(e)
            ScriptExecution(engine.id, source, null, e)
        }
    }
    
    /**
     * Stop the running script.
     */
    fun stopAll(): Int {
        return if (!engine.isDestroyed) {
            engine.forceStop()
            1
        } else {
            0
        }
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

