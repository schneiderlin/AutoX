package com.autocljs.runtime

import android.content.Context
import com.autocljs.runtime.api.Console

/**
 * Minimal script runtime for Phase 1.
 * Provides basic runtime environment with Console API.
 */
class ScriptRuntime(private val context: Context) {
    
    val console: Console = com.autocljs.runtime.api.SimpleConsole("AutoCLJS")
    
    companion object {
        @Volatile
        private var applicationContext: Context? = null
        
        fun setApplicationContext(context: Context) {
            applicationContext = context.applicationContext
        }
        
        fun getApplicationContext(): Context? {
            return applicationContext
        }
    }
}

