package com.autocljs.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.interop.callback.IV8ModuleResolver
import com.caoccao.javet.values.reference.IV8Module
import com.caoccao.javet.values.reference.V8Module
import java.io.File
import java.io.FileNotFoundException

/**
 * Simplified module resolver for Node.js/V8 engine.
 * Supports loading modules from:
 * 1. Pre-bundled modules assets (modules/) - Node.js-like module structure
 * 2. File system (module directory)
 * 3. Relative paths
 * 
 * Module resolution follows Node.js conventions:
 * - Package name: "utils" → modules/utils/index.mjs or modules/utils/index.js
 * - Package path: "squint-runtime/core.js" → modules/squint-runtime/core.js
 * - Supports both .mjs and .js extensions
 */
class SimpleNodeModuleResolver(
    private val runtime: NodeRuntime,
    private val context: Context,
    private val moduleDirectory: File
) : IV8ModuleResolver {
    
    private val esModuleCache = mutableMapOf<String, IV8Module>()
    
    companion object {
        private const val TAG = "SimpleNodeModuleResolver"
        private const val MODULES_PREFIX = "modules/"
        
        /**
         * Normalize an asset path by resolving . and .. segments.
         * e.g., "modules/squint-cljs/./src/squint/core.js" -> "modules/squint-cljs/src/squint/core.js"
         */
        fun normalizeAssetPath(path: String): String {
            val segments = path.split("/").toMutableList()
            val result = mutableListOf<String>()
            
            for (segment in segments) {
                when (segment) {
                    ".", "" -> { /* skip */ }
                    ".." -> if (result.isNotEmpty()) result.removeAt(result.lastIndex)
                    else -> result.add(segment)
                }
            }
            
            return result.joinToString("/")
        }
        
        /**
         * Check if a file is an ES module.
         */
        fun isEsModule(file: File): Boolean {
            return file.name.endsWith(".mjs") || 
                   file.name.endsWith(".js") ||
                   file.readText().contains("import ") || 
                   file.readText().contains("export ")
        }
        
        /**
         * Compile a V8 module from source code.
         */
        fun compileV8Module(v8Runtime: V8Runtime, source: String, resourceName: String): V8Module {
            val executor = v8Runtime.getExecutor(source)
            executor.v8ScriptOrigin.resourceName = resourceName
            return executor.setModule(true).compileV8Module()
        }
    }
    
    /**
     * Resolve a module by resource name.
     */
    override fun resolve(
        v8Runtime: V8Runtime,
        resourceName: String,
        v8ModuleReferrer: IV8Module
    ): IV8Module? {
        Log.d(TAG, "resolve: $resourceName referrer: ${v8ModuleReferrer.resourceName}")
        
        // Check cache first
        esModuleCache[resourceName]?.let { return it }
        
        // Handle relative paths
        if (resourceName.startsWith("./") || resourceName.startsWith("../")) {
            val referrerPath = v8ModuleReferrer.resourceName
            
            // Check if the referrer was loaded from assets (resource name starts with "modules/")
            if (referrerPath.startsWith(MODULES_PREFIX)) {
                // Resolve relative path within assets
                val baseDir = referrerPath.substringBeforeLast("/")
                val resolvedAssetPath = normalizeAssetPath("$baseDir/$resourceName")
                Log.d(TAG, "Resolving relative import from assets: $resolvedAssetPath")
                
                val module = loadFromAssets(v8Runtime, resolvedAssetPath)
                if (module != null) {
                    return module
                }
                // Fall through to try filesystem resolution as well
            }
            
            val baseFile = File(referrerPath).parentFile ?: File("/")
            val resolvedFile = File(baseFile, resourceName).canonicalFile
            return parsingModule(v8Runtime, resolvedFile)
        }
        
        // Handle absolute paths
        if (resourceName.startsWith("/")) {
            return parsingModule(v8Runtime, File(resourceName))
        }
        
        // Handle module names (e.g., "utils", "squint-runtime/core.js")
        // Try loading from pre-bundled modules assets (Node.js-like structure)
        val moduleFromAssets = resolveModuleFromAssets(v8Runtime, resourceName)
        if (moduleFromAssets != null) {
            return moduleFromAssets
        }
        
        // Try loading from module directory (filesDir/v7_modules)
        val moduleFromDirectory = resolveModuleFromDirectory(v8Runtime, resourceName)
        if (moduleFromDirectory != null) {
            return moduleFromDirectory
        }
        
        // Try as relative to referrer
        val referrerPath = v8ModuleReferrer.resourceName
        val baseFile = File(referrerPath).parentFile ?: File("/")
        val relativeFile = File(baseFile, resourceName)
        if (relativeFile.exists()) {
            return parsingModule(v8Runtime, relativeFile)
        }
        
        Log.w(TAG, "Module not found: $resourceName")
        return null
    }
    
    /**
     * Resolve a module from Android assets using Node.js-like resolution.
     * Supports:
     * - Package name: "utils" → modules/utils/index.mjs, modules/utils/index.js, modules/utils/utils.mjs, etc.
     * - Package path: "squint-runtime/core.js" → modules/squint-runtime/core.js, modules/squint-runtime/core.mjs
     */
    private fun resolveModuleFromAssets(v8Runtime: V8Runtime, resourceName: String): IV8Module? {
        // Check if it's a package path (contains slash)
        if (resourceName.contains("/")) {
            // Package path: "squint-runtime/core.js" or "utils/helper"
            val hasExtension = resourceName.endsWith(".mjs") || resourceName.endsWith(".js")
            val paths = if (hasExtension) {
                // Already has extension, try as-is
                listOf("$MODULES_PREFIX$resourceName")
            } else {
                // No extension, try with .mjs and .js
                listOf(
                    "$MODULES_PREFIX$resourceName.mjs",
                    "$MODULES_PREFIX$resourceName.js"
                )
            }
            
            for (path in paths) {
                val module = loadFromAssets(v8Runtime, path)
                if (module != null) {
                    return module
                }
            }
        } else {
            // Package name: "utils" - try multiple resolution strategies
            val packageName = resourceName
            val paths = listOf(
                // Try index.mjs/index.js first (Node.js convention)
                "$MODULES_PREFIX$packageName/index.mjs",
                "$MODULES_PREFIX$packageName/index.js",
                // Try package name as file
                "$MODULES_PREFIX$packageName/$packageName.mjs",
                "$MODULES_PREFIX$packageName/$packageName.js",
                // Try flat file
                "$MODULES_PREFIX$packageName.mjs",
                "$MODULES_PREFIX$packageName.js"
            )
            
            for (path in paths) {
                val module = loadFromAssets(v8Runtime, path)
                if (module != null) {
                    return module
                }
            }
        }
        
        return null
    }
    
    /**
     * Resolve a module from the module directory using Node.js-like resolution.
     */
    private fun resolveModuleFromDirectory(v8Runtime: V8Runtime, resourceName: String): IV8Module? {
        // Check if it's a package path (contains slash)
        if (resourceName.contains("/")) {
            // Package path: "squint-runtime/core.js"
            val hasExtension = resourceName.endsWith(".mjs") || resourceName.endsWith(".js")
            val paths = if (hasExtension) {
                // Already has extension, try as-is
                listOf(File(moduleDirectory, resourceName))
            } else {
                // No extension, try with .mjs and .js
                listOf(
                    File(moduleDirectory, "$resourceName.mjs"),
                    File(moduleDirectory, "$resourceName.js")
                )
            }
            
            for (file in paths) {
                if (file.exists()) {
                    return parsingModule(v8Runtime, file)
                }
            }
        } else {
            // Package name: "utils" - try multiple resolution strategies
            val packageName = resourceName
            val paths = listOf(
                // Try index.mjs/index.js first (Node.js convention)
                File(moduleDirectory, "$packageName/index.mjs"),
                File(moduleDirectory, "$packageName/index.js"),
                // Try package name as file
                File(moduleDirectory, "$packageName/$packageName.mjs"),
                File(moduleDirectory, "$packageName/$packageName.js"),
                // Try flat file
                File(moduleDirectory, "$packageName.mjs"),
                File(moduleDirectory, "$packageName.js")
            )
            
            for (file in paths) {
                if (file.exists()) {
                    return parsingModule(v8Runtime, file)
                }
            }
        }
        
        return null
    }
    
    /**
     * Load a module from Android assets.
     */
    private fun loadFromAssets(v8Runtime: V8Runtime, assetPath: String): IV8Module? {
        return try {
            val inputStream = context.assets.open(assetPath)
            val source = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()
            
            val module = compileV8Module(v8Runtime, source, assetPath)
            addCacheModule(module)
            module
        } catch (e: FileNotFoundException) {
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from assets: $assetPath", e)
            null
        }
    }
    
    /**
     * Parse and load a module from a file.
     */
    private fun parsingModule(v8Runtime: V8Runtime, file: File): IV8Module? {
        if (!file.exists()) {
            return null
        }
        
        val cacheKey = file.canonicalPath
        esModuleCache[cacheKey]?.let { return it }
        
        return try {
            val source = file.readText()
            val isEsModule = isEsModule(file)
            
            val module = if (isEsModule) {
                compileV8Module(v8Runtime, source, cacheKey)
            } else {
                // For CommonJS, we'd need to use require, but for now just compile as ES module
                compileV8Module(v8Runtime, source, cacheKey)
            }
            
            addCacheModule(module)
            module
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing module: ${file.path}", e)
            null
        }
    }
    
    /**
     * Add a module to the cache.
     */
    fun addCacheModule(module: IV8Module) {
        esModuleCache[module.resourceName] = module
    }
    
    /**
     * Remove a module from the cache by resource name.
     * This is needed to allow re-execution of scripts with the same name.
     */
    fun removeCacheModule(resourceName: String) {
        esModuleCache.remove(resourceName)
    }
    
    /**
     * Create a require function for CommonJS modules (simplified).
     */
    val require: com.caoccao.javet.values.reference.V8ValueFunction by lazy {
        runtime.getNodeModule(com.caoccao.javet.node.modules.NodeModuleModule::class.java)
            .moduleObject
            .invoke(com.caoccao.javet.node.modules.NodeModuleModule.FUNCTION_CREATE_REQUIRE, "")
    }
    
    /**
     * Clean up resources.
     */
    fun recycle() {
        esModuleCache.clear()
    }
}

