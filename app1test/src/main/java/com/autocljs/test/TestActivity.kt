package com.autocljs.test

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.autocljs.layout.getLayout
import com.autocljs.layout.layoutToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.app.ActivityCompat
import com.autocljs.ScriptEngineService
import com.autocljs.accessibility.AccessibilityBridgeImpl
import com.autocljs.accessibility.AccessibilityConfig
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.script.NodeScriptSource
import com.autocljs.script.StringScriptSource
import com.autocljs.util.ScreenMetrics
import com.autocljs.util.UiHandler
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

/**
 * Test Activity for fetching and executing scripts from HTTP endpoint.
 * 
 * Features:
 * - Configurable HTTP endpoint URL
 * - Fetch scripts from server
 * - Display list of available scripts
 * - Execute selected scripts
 */
class TestActivity : AppCompatActivity() {

    private companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    }

    private lateinit var logView: TextView
    private lateinit var urlEditText: EditText
    private lateinit var fetchButton: Button
    private lateinit var floatingButton: Button
    private lateinit var testLayoutButton: Button
    private lateinit var scriptListView: ListView
    private lateinit var runtime: ScriptRuntime
    private lateinit var scriptEngineService: ScriptEngineService
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var gson: Gson
    
    private var scripts: List<Script> = emptyList()
    
    // Data class for script JSON parsing
    data class Script(
        val name: String, 
        val code: String,
        val modules: List<Module>? = null
    ) {
        data class Module(val name: String, val code: String)
    }
    
    // Wrapper class for server response (server wraps array in object)
    data class QueryResponse(
        @SerializedName("success?") val success: Boolean = false,
        val result: List<Script>? = null
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize runtime
        runtime = ScriptRuntime(this)
        
        // Initialize script engine service with shared runtime
        scriptEngineService = ScriptEngineService.Builder(this)
            .setRuntime(runtime)
            .build()
        
        // Initialize HTTP client and JSON parser
        okHttpClient = OkHttpClient()
        gson = Gson()
        
        // Create UI
        createUI()
    }
    
    private fun createUI() {
        // URL input field
        urlEditText = EditText(this).apply {
            hint = "Enter script server base URL"
            setText("http://10.0.2.2:3000") // Default for Android emulator
            textSize = 14f
            setPadding(16, 16, 16, 16)
        }
        
        // Fetch button
        fetchButton = Button(this).apply {
            text = "Fetch Scripts"
            setOnClickListener { fetchScripts() }
        }

        // Floating window button
        floatingButton = Button(this).apply {
            text = "Show Floating Button"
            setOnClickListener { showFloatingButton() }
        }

        // Test Layout Inspector button
        testLayoutButton = Button(this).apply {
            text = "Test Layout Inspector"
            setOnClickListener { testLayoutInspector() }
        }

        // Script list view
        scriptListView = ListView(this).apply {
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                executeScript(scripts[position])
            }
        }
        
        // Log view
        logView = TextView(this).apply {
            text = "AutoCLJS Test App\n\nEnter URL and tap 'Fetch Scripts' to load scripts"
            textSize = 12f
            setPadding(16, 16, 16, 16)
        }
        
        val scrollView = ScrollView(this).apply {
            addView(logView)
        }
        
        // Main layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(urlEditText)
            addView(fetchButton)
            addView(floatingButton)
            addView(testLayoutButton)
            addView(scriptListView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ))
            addView(scrollView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ))
        }
        
        setContentView(layout)
    }
    
    private fun fetchScripts() {
        val baseUrl = urlEditText.text.toString().trim()
        
        if (baseUrl.isEmpty()) {
            log("ERROR: Please enter a URL")
            return
        }
        
        // Construct the query endpoint URL
        val queryUrl = if (baseUrl.endsWith("/")) {
            "${baseUrl}api/query"
        } else {
            "$baseUrl/api/query"
        }
        
        log("Fetching scripts from: $queryUrl")
        fetchButton.isEnabled = false
        fetchButton.text = "Loading..."
        
        // Create JSON request body
        val requestBody = """
            {
                "query/kind": "query/scripts",
                "query/data": {
                    "page": 1
                }
            }
        """.trimIndent()
        
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = requestBody.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(queryUrl)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    log("ERROR: Failed to fetch scripts: ${e.message}")
                    fetchButton.isEnabled = true
                    fetchButton.text = "Fetch Scripts"
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        log("ERROR: HTTP ${response.code}: ${response.message}")
                        fetchButton.isEnabled = true
                        fetchButton.text = "Fetch Scripts"
                    }
                    return
                }
                
                try {
                    val responseBody = response.body?.string()
                    if (responseBody == null) {
                        runOnUiThread {
                            log("ERROR: Empty response from server")
                            fetchButton.isEnabled = true
                            fetchButton.text = "Fetch Scripts"
                        }
                        return
                    }
                    
                    // Parse JSON response object
                    val queryResponse: QueryResponse = gson.fromJson(responseBody, QueryResponse::class.java)
                    
                    if (!queryResponse.success) {
                        runOnUiThread {
                            log("ERROR: Server returned success=false")
                            fetchButton.isEnabled = true
                            fetchButton.text = "Fetch Scripts"
                        }
                        return
                    }
                    
                    val fetchedScripts = queryResponse.result ?: emptyList()
                    
                    runOnUiThread {
                        scripts = fetchedScripts
                        updateScriptList()
                        log("Successfully fetched ${scripts.size} script(s)")
                        fetchButton.isEnabled = true
                        fetchButton.text = "Fetch Scripts"
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        log("ERROR: Failed to parse JSON: ${e.message}")
                        e.printStackTrace()
                        fetchButton.isEnabled = true
                        fetchButton.text = "Fetch Scripts"
                    }
                }
            }
        })
    }
    
    private fun updateScriptList() {
        val scriptNames = scripts.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scriptNames)
        scriptListView.adapter = adapter
    }
    
    private fun executeScript(script: Script) {
        log("=== Executing Script: ${script.name} ===")
        
        // Check if script uses automation (contains 'auto.')
        val usesAutomation = script.code.contains("auto.")
        
        if (usesAutomation) {
            // Run on background thread for automation scripts
            Thread {
                executeScriptWithAutomation(script)
            }.start()
        } else {
            // Execute simple scripts on current thread
            executeScriptSimple(script)
        }
    }
    
    private fun executeScriptSimple(script: Script) {
        try {
            // Write additional modules to cacheDir so they can be found by relative imports
            // NodeScriptEngine writes the entry point to cacheDir, so modules must be there too
            script.modules?.forEach { module ->
                val moduleFile = File(cacheDir, module.name)
                moduleFile.writeText(module.code)
                log("Written module: ${module.name} to cacheDir")
            }
            
            // Use NodeScriptSource to test NodeScriptEngine
            // NodeScriptEngine will write the entry point to cacheDir with a unique name
            val source = NodeScriptSource("${script.name}.mjs", script.code)
            val execution = scriptEngineService.execute(source)
            
            val exception = execution.exception
            if (exception != null) {
                log("ERROR: ${exception.message}")
                exception.printStackTrace()
            } else {
                log("Script executed successfully!")
                log("Check logcat for console output (tag: 'AutoCLJS')")
            }
        } catch (e: Exception) {
            log("ERROR executing script: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun executeScriptWithAutomation(script: Script) {
        try {
            // Initialize screen metrics
            ScreenMetrics.initIfNeeded(this@TestActivity)
            log("Screen metrics initialized")
            
            // Create accessibility bridge
            val config = AccessibilityConfig()
            val uiHandler = UiHandler(this@TestActivity)
            val bridge = AccessibilityBridgeImpl(this@TestActivity, config, uiHandler)
            log("Accessibility bridge created")
            
            // Ensure service is enabled
            log("Checking accessibility service...")
            bridge.ensureServiceEnabled()
            log("Accessibility service is enabled ✓")
            
            // Initialize automation (required before JavaScript can use auto API)
            runtime.initAutomation(bridge)
            log("Automation initialized")
            
            // Write additional modules to cacheDir so they can be found by relative imports
            // NodeScriptEngine writes the entry point to cacheDir, so modules must be there too
            script.modules?.forEach { module ->
                val moduleFile = File(cacheDir, module.name)
                moduleFile.writeText(module.code)
                log("Written module: ${module.name} to cacheDir")
            }
            
            // Execute JavaScript code using NodeScriptEngine
            // NodeScriptEngine will write the entry point to cacheDir with a unique name
            val source = NodeScriptSource("${script.name}.mjs", script.code)
            val execution = scriptEngineService.execute(source)
            
            val exception = execution.exception
            if (exception != null) {
                log("ERROR: ${exception.message}")
                exception.printStackTrace()
            } else {
                log("Script executed successfully!")
                log("Check logcat for console output")
            }
        } catch (e: IllegalStateException) {
            log("ERROR: ${e.message}")
            log("Please enable the accessibility service in Android Settings:")
            log("Settings → Accessibility → AutoCLJS automation service")
        } catch (e: Exception) {
            log("ERROR: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun log(message: String) {
        runOnUiThread {
            logView.text = "${logView.text}\n$message"
        }
        android.util.Log.d("TestActivity", message)
    }

    private fun showFloatingButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // Request overlay permission
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                log("Requesting overlay permission...")
            } else {
                // Permission already granted, start the floating window service
                startFloatingWindowService()
            }
        } else {
            // Pre-Marshmallow, permission is granted at install time
            startFloatingWindowService()
        }
    }

    private fun startFloatingWindowService() {
        val intent = Intent(this, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        log("Floating window service started!")
        log("Look for the floating button on your screen")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    log("Overlay permission granted!")
                    startFloatingWindowService()
                } else {
                    log("Overlay permission denied. Floating button cannot be shown.")
                }
            }
        }
    }

    /**
     * Test Layout Inspector - captures current layout and sends to script server.
     */
    private fun testLayoutInspector() {
        log("Capturing layout...")

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Get the layout using the function from app1 module
                    val layout = getLayout()
                    if (layout == null) {
                        return@withContext "ERROR: Accessibility service not enabled or no active window"
                    }

                    // Convert to JSON
                    val layoutJson = layoutToJson(layout)

                    // Send to script server
                    val baseUrl = urlEditText.text.toString().trim()
                    if (baseUrl.isEmpty()) {
                        return@withContext "ERROR: Please enter a server URL"
                    }

                    val commandUrl = if (baseUrl.endsWith("/")) {
                        "${baseUrl}api/command"
                    } else {
                        "$baseUrl/api/command"
                    }

                    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())

                    // Build command payload using proper JSON structure
                    val commandData = mapOf(
                        "command/kind" to "command/save-layout",
                        "command/data" to mapOf(
                            "layout-data" to layoutJson,
                            "timestamp" to timestamp
                        )
                    )
                    val commandBody = gson.toJson(commandData)

                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val body = commandBody.toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url(commandUrl)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build()

                    val response = okHttpClient.newCall(request).execute()

                    if (!response.isSuccessful) {
                        return@withContext "ERROR: Server returned ${response.code}"
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val responseObj = gson.fromJson(responseBody, Map::class.java)
                        val file = responseObj["file"]?.toString() ?: "unknown"
                        "SUCCESS: Saved to $file"
                    } else {
                        "ERROR: Empty response from server"
                    }
                }

                log(result)
            } catch (e: Exception) {
                log("ERROR: ${e.message}")
                android.util.Log.e("TestActivity", "Layout inspector error", e)
            }
        }
    }
}
