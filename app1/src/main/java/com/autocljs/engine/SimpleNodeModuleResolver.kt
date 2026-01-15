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
 * 1. Assets (squint-runtime/)
 * 2. File system (cache directory)
 * 3. Relative paths
 */
class SimpleNodeModuleResolver(
    private val runtime: NodeRuntime,
    private val context: Context,
    private val moduleDirectory: File
) : IV8ModuleResolver {
    
    private val esModuleCache = mutableMapOf<String, IV8Module>()
    
    companion object {
        private const val TAG = "SimpleNodeModuleResolver"
        private const val ASSETS_PREFIX = "squint-runtime/"
        
        /**
         * Check if a file is an ES module.
         */
        fun isEsModule(file: File): Boolean {
            return file.name.endsWith(".mjs") || 
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
            val baseFile = File(referrerPath).parentFile ?: File("/")
            val resolvedFile = File(baseFile, resourceName).canonicalFile
            return parsingModule(v8Runtime, resolvedFile)
        }
        
        // Handle absolute paths
        if (resourceName.startsWith("/")) {
            return parsingModule(v8Runtime, File(resourceName))
        }
        
        // Handle module names (e.g., "squint-cljs/core.js")
        // Try loading from assets first
        val assetPath = ASSETS_PREFIX + resourceName
        val assetModule = loadFromAssets(v8Runtime, assetPath)
        if (assetModule != null) {
            return assetModule
        }
        
        // Try loading from module directory
        val moduleFile = File(moduleDirectory, resourceName)
        if (moduleFile.exists()) {
            return parsingModule(v8Runtime, moduleFile)
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

