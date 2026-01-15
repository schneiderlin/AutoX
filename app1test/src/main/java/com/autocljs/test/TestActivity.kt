package com.autocljs.test

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.autocljs.ScriptEngineService
import com.autocljs.accessibility.AccessibilityBridgeImpl
import com.autocljs.accessibility.AccessibilityConfig
import com.autocljs.runtime.ScriptRuntime
import com.autocljs.script.StringScriptSource
import com.autocljs.util.ScreenMetrics
import com.autocljs.util.UiHandler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
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
    
    private lateinit var logView: TextView
    private lateinit var urlEditText: EditText
    private lateinit var fetchButton: Button
    private lateinit var scriptListView: ListView
    private lateinit var runtime: ScriptRuntime
    private lateinit var scriptEngineService: ScriptEngineService
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var gson: Gson
    
    private var scripts: List<Script> = emptyList()
    
    // Data class for script JSON parsing
    data class Script(val name: String, val code: String)
    
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
            hint = "Enter script endpoint URL"
            setText("http://10.0.2.2:3000/scripts") // Default for Android emulator
            textSize = 14f
            setPadding(16, 16, 16, 16)
        }
        
        // Fetch button
        fetchButton = Button(this).apply {
            text = "Fetch Scripts"
            setOnClickListener { fetchScripts() }
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
        val urlString = urlEditText.text.toString().trim()
        
        if (urlString.isEmpty()) {
            log("ERROR: Please enter a URL")
            return
        }
        
        log("Fetching scripts from: $urlString")
        fetchButton.isEnabled = false
        fetchButton.text = "Loading..."
        
        val request = Request.Builder()
            .url(urlString)
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
                    
                    // Parse JSON array
                    val listType = object : TypeToken<List<Script>>() {}.type
                    val fetchedScripts: List<Script> = gson.fromJson(responseBody, listType)
                    
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
            val source = StringScriptSource("${script.name}.js", script.code)
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
            
            // Execute JavaScript code
            val source = StringScriptSource("${script.name}.js", script.code)
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
}
