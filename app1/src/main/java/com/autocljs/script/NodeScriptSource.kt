package com.autocljs.script

import com.autocljs.engine.NodeScriptEngine
import java.io.File

/**
 * Script source for ES module scripts (Node.js/V8 engine).
 * 
 * Used for .mjs files and ES modules compiled from ClojureScript.
 */
class NodeScriptSource(
    name: String,
    val script: String
) : ScriptSource(name) {
    
    override val engineName: String = NodeScriptEngine.ID
    
    /**
     * Create from a file path (for future use with file-based scripts).
     */
    constructor(file: File) : this(file.name, file.readText())
    
    override fun toString(): String {
        return "NodeScript@$name"
    }
}

