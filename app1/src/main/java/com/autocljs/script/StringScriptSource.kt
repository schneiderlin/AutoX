package com.autocljs.script

/**
 * Script source from a string.
 * 
 * Note: This receives compiled JavaScript code.
 * ClojureScript is compiled to JavaScript before reaching this library.
 */
class StringScriptSource(name: String, val script: String) : ScriptSource(name) {
    override val engineName: String = "javascript"
}

