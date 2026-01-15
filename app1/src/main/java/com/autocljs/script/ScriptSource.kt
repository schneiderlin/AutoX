package com.autocljs.script

import java.io.Serializable

/**
 * Abstract base class for script sources.
 * Represents a source of script code that can be executed.
 */
abstract class ScriptSource(val name: String) : Serializable {
    abstract val engineName: String
}

