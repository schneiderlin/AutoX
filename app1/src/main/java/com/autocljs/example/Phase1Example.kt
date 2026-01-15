package com.autocljs.example

import android.content.Context
import android.util.Log
import com.autocljs.ScriptEngineService
import com.autocljs.script.StringScriptSource

/**
 * Example demonstrating Phase 1 functionality:
 * - Script execution infrastructure
 * - Console output
 * - Basic script source handling
 * 
 * Note: This receives compiled JavaScript code.
 * ClojureScript is compiled to JavaScript before reaching this library.
 * 
 * This shows how to execute a simple "Hello World" script.
 */
object Phase1Example {
    
    private const val TAG = "Phase1Example"
    
    /**
     * Example: Execute a simple "Hello World" script.
     */
    fun executeHelloWorld(context: Context) {
        Log.d(TAG, "=== Phase 1 Example: Hello World ===")
        
        // Create script engine service
        val service = ScriptEngineService.Builder(context)
            .build()
        
        // Create a simple script source
        // Note: This is compiled JavaScript code (ClojureScript was compiled elsewhere)
        val script = StringScriptSource(
            name = "hello-world",
            script = """
                console.log("Hello, World!");
                console.log("This is AutoCLJS Phase 1");
            """.trimIndent()
        )
        
        // Execute the script
        val execution = service.execute(script)
        
        // Check results
        if (execution.exception != null) {
            Log.e(TAG, "Script execution failed", execution.exception)
        } else {
            Log.d(TAG, "Script executed successfully. Result: ${execution.result}")
        }
        
        Log.d(TAG, "=== End Example ===")
    }
    
    /**
     * Example: Execute multiple scripts.
     */
    fun executeMultipleScripts(context: Context) {
        Log.d(TAG, "=== Phase 1 Example: Multiple Scripts ===")
        
        val service = ScriptEngineService.Builder(context)
            .build()
        
        // Note: These are compiled JavaScript (ClojureScript was compiled elsewhere)
        val scripts = listOf(
            StringScriptSource("script1", "console.log('Script 1');"),
            StringScriptSource("script2", "console.log('Script 2');"),
            StringScriptSource("script3", "console.log('Script 3');")
        )
        
        scripts.forEach { script ->
            val execution = service.execute(script)
            Log.d(TAG, "Executed ${script.name}: ${execution.result}")
        }
        
        Log.d(TAG, "=== End Example ===")
    }
}

