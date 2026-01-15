package com.autocljs.engine

import android.content.Context
import android.util.Log
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.runtime.api.NodeAuto
import com.autocljs.runtime.api.NodeConsole
import com.autocljs.script.NodeScriptSource
import com.autocljs.script.ScriptSource
import com.caoccao.javet.entities.JavetEntityError
import com.caoccao.javet.enums.V8AwaitMode
import com.caoccao.javet.exceptions.BaseJavetScriptingException
import com.caoccao.javet.exceptions.JavetScriptingError
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.callback.IV8ModuleResolver
import com.caoccao.javet.interop.converters.JavetProxyConverter
import com.caoccao.javet.node.modules.NodeModuleModule
import com.caoccao.javet.node.modules.NodeModuleProcess
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.reference.IV8Module
import com.caoccao.javet.values.reference.V8Module
import com.caoccao.javet.values.reference.V8ValueError
import com.caoccao.javet.values.reference.V8ValuePromise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.locks.ReentrantLock

/**
 * NodeScriptEngine implementation for app1 module.
 * 
 * Supports ES module execution using Javet/V8.
 * Exposes console and auto APIs similar to ClojureScriptEngine.
 */
class NodeScriptEngine(
    private val context: Context,
    private val sharedRuntime: ScriptRuntime? = null
) : ScriptEngine.AbstractScriptEngine<ScriptSource>() {
    
    val runtime: NodeRuntime = V8Host.getNodeInstance().createV8Runtime()
    
    private var scriptRuntime: ScriptRuntime? = null
    private var isInitialized = false
    private val tags = mutableMapOf<String, Any?>()
    private val v8Locker = ReentrantLock()
    private val moduleDirectory = getModuleDirectory(context)
    private val resultListener = PromiseListener()
    private var console: NodeConsole? = null
    private var auto: NodeAuto? = null
    private val converter = JavetProxyConverter().apply {
        config.setProxyMapEnabled(true)
        config.setProxySetEnabled(true)
        config.setProxyListEnabled(true)
    }
    val scope = CoroutineScope(Dispatchers.Default)
    private val moduleResolver = SimpleNodeModuleResolver(runtime, context, moduleDirectory)
    
    companion object {
        const val ID = "com.autocljs.engine.NodeScriptEngine"
        private const val TAG = "NodeScriptEngine"
        
        fun getModuleDirectory(context: Context): File {
            return File(context.filesDir, "v7_modules")
        }
    }
    
    init {
        Log.i(TAG, "Node version: ${runtime.version}")
    }
    
    @Synchronized
    override fun init() {
        if (!isInitialized) {
            scriptRuntime = sharedRuntime ?: ScriptRuntime(context)
            
            // Initialize console and auto APIs
            console = NodeConsole(scriptRuntime!!.console)
            auto = scriptRuntime?.let { NodeAuto(it) }
            
            runtime.converter = converter
            runtime.allowEval(true)
            runtime.isStopping = true
            
            // Set up module resolver
            runtime.v8ModuleResolver = moduleResolver
            
            // Initialize process handlers
            initializeProcess()
            
            // Expose console and auto APIs
            initializeApi()
            
            isInitialized = true
            Log.d(TAG, "NodeScriptEngine initialized")
        }
    }
    
    /**
     * Initialize Node.js process handlers.
     */
    private fun initializeProcess() {
        runtime.getExecutor(
            """
            (()=>{
                let c = 0
                process.on('uncaughtException', (err) => {
                    c++
                    if (c > 5) process.abort()
                    throw err
                })
                process.exit = function(code) {
                    if (code > 0){
                        throw new Error('exit with code: ' + code)
                    }
                }
            })()
        """.trimIndent()
        ).executeVoid()
    }
    
    /**
     * Initialize console and auto APIs in the global scope.
     */
    private fun initializeApi() = runtime.globalObject.use { global ->
        // Expose console
        console?.install(runtime, global)
        
        // Expose auto
        auto?.install(runtime, global)
        
        Log.d(TAG, "Console and Auto APIs exposed")
    }
    
    override fun put(name: String, value: Any?) {
        tags[name] = value
        // Also set in V8 global scope if needed
        runtime.globalObject.use { global ->
            global.set(name, value)
        }
    }
    
    override fun execute(scriptSource: ScriptSource): Any? = runBlocking {
        if (!isInitialized) {
            init()
        }
        
        check(scriptSource is NodeScriptSource) { 
            "scriptSource must be NodeScriptSource, got ${scriptSource::class.java.name}" 
        }
        
        Log.d(TAG, "Executing ES module: ${scriptSource.name}")
        
        try {
            // Create a temporary file for the script
            // Use .mjs extension if not already present
            val fileName = if (scriptSource.name.endsWith(".mjs")) {
                scriptSource.name
            } else {
                "${scriptSource.name}.mjs"
            }
            val tempFile = File(context.cacheDir, "temp_$fileName")
            tempFile.writeText(scriptSource.script)
            
            initializeModule(tempFile).use {
                if (it is V8ValuePromise) {
                    it.register(resultListener)
                } else {
                    resultListener.onFulfilled(it)
                }
            }
            
            // Wait for async operations
            while (scope.isActive) {
                if (runtime.await(V8AwaitMode.RunNoWait)) {
                    Thread.sleep(1)
                    continue
                } else {
                    break
                }
            }
            
            // Clean up temp file
            tempFile.delete()
            
            if (resultListener.isFulfilledCalled) {
                return@runBlocking resultListener.await()
            } else if (resultListener.isRejectedCalled) {
                exceptionHandling(resultListener.await())
            } else {
                return@runBlocking null
            }
        } catch (e: Throwable) {
            exceptionHandling(e)
        }
    }
    
    /**
     * Initialize and execute a module file.
     */
    private fun initializeModule(file: File): V8Value {
        val parentFile = file.parentFile ?: File("/")
        runtime.getNodeModule(NodeModuleProcess::class.java).workingDirectory = parentFile
        
        // Remove default require to use our custom resolver
        runtime.globalObject.delete(NodeModuleModule.PROPERTY_REQUIRE)
        
        val source = file.readText()
        
        // Check if it's an ES module (ends with .mjs or has import/export)
        val isEsModule = file.name.endsWith(".mjs") || 
                        source.contains("import ") || 
                        source.contains("export ")
        
        return if (isEsModule) {
            // ES module
            val module = SimpleNodeModuleResolver.compileV8Module(
                runtime, 
                source, 
                file.path
            )
            moduleResolver.addCacheModule(module)
            module.execute()
        } else if (file.name.endsWith(".cjs") || source.contains("require(") || source.contains("module.exports")) {
            // CommonJS
            moduleResolver.require.call(null, runtime.createV8ValueString(file.path))
        } else {
            // Plain JavaScript - execute directly
            runtime.getExecutor(source).execute()
        }
    }
    
    /**
     * Handle exceptions from script execution.
     */
    private fun exceptionHandling(e: Any?) {
        when (e) {
            is V8ValueError -> {
                console?.error(e.stack)
                throw RuntimeException(e.message)
            }
            
            is JavetEntityError -> {
                console?.error(e.stack)
                throw RuntimeException(e.message)
            }
            
            is JavetScriptingError -> {
                console?.error(e.stack)
                throw RuntimeException(e.message)
            }
            
            is BaseJavetScriptingException -> {
                if (e.cause != null) {
                    exceptionHandling(e.cause)
                } else {
                    e.scriptingError.let {
                        console?.error("${it.stack}\n  at ${it.resourceName}:${it.lineNumber}:${it.startColumn}")
                    }
                    throw e
                }
            }
            
            is Throwable -> {
                console?.error(e.stackTraceToString())
                throw e
            }
            
            else -> throw RuntimeException(e.toString())
        }
    }
    
    override fun forceStop() {
        Log.d(TAG, "forceStop() called")
        resultListener.cancel()
        if (runtime.isInUse) {
            runtime.terminateExecution()
        }
        if (scope.isActive) {
            scope.cancel("force stop")
        }
    }
    
    override fun destroy() {
        super.destroy()
        
        if (scope.isActive) {
            scope.cancel()
        }
        
        // Clean up APIs
        runtime.globalObject.use { global ->
            console?.recycle(runtime, global)
            auto?.recycle(runtime, global)
        }
        
        // Clean up module resolver
        moduleResolver.recycle()
        
        if (!runtime.isClosed) {
            runtime.lowMemoryNotification()
            runtime.close()
        }
        
        isInitialized = false
        Log.d(TAG, "NodeScriptEngine destroyed")
    }
}

/**
 * Simple promise listener for async operations.
 */
private class PromiseListener : com.caoccao.javet.values.reference.IV8ValuePromise.IListener {
    private val result = kotlinx.coroutines.CompletableDeferred<Any?>()
    var stack: String? = null
    
    @Volatile
    var isFulfilledCalled = false
    
    @Volatile
    var isRejectedCalled = false
    
    override fun onFulfilled(v8Value: V8Value?) {
        isFulfilledCalled = true
        if (v8Value != null) {
            result.complete(v8Value.v8Runtime.converter.toObject(v8Value))
        } else {
            result.complete(null)
        }
    }
    
    override fun onRejected(v8Value: V8Value?) {
        isRejectedCalled = true
        if (v8Value is V8ValueError) {
            stack = v8Value.stack
        }
        result.complete(v8Value?.v8Runtime?.converter?.toObject(v8Value))
    }
    
    override fun onCatch(v8Value: V8Value?) {
        // Handle catch if needed
    }
    
    suspend fun await(): Any? {
        return result.await()
    }
    
    fun cancel() = result.cancel()
}

