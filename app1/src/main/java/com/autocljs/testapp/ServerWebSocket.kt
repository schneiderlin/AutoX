package com.autocljs.testapp

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class ServerWebSocket {
    companion object {
        private const val TAG = "ServerWebSocket"
        private const val DEFAULT_SERVER_URL = "ws://localhost:3000/ws"
    }

    private val gson = Gson()
    private var client: HttpClient? = null
    private var session: DefaultClientWebSocketSession? = null
    private var currentClientId: String? = null

    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState = _connectionState.asSharedFlow()

    private val _messages = MutableSharedFlow<JsonObject>()
    val messages = _messages.asSharedFlow()

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    var socketTimeoutMillis = 10000L
    var pingInterval = 15000L

    suspend fun connect(
        serverUrl: String = DEFAULT_SERVER_URL,
        clientId: String = "android_client"
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            _connectionState.emit(ConnectionState.CONNECTING)
            currentClientId = clientId

            try {
                client = HttpClient(OkHttp) {
                    install(HttpTimeout) {
                        connectTimeoutMillis = 10000
                        socketTimeoutMillis = this@ServerWebSocket.socketTimeoutMillis
                    }
                    install(WebSockets) {
                        this.pingInterval = this@ServerWebSocket.pingInterval
                        maxFrameSize = Long.MAX_VALUE
                    }
                }

                val url = "$serverUrl?client-id=$clientId"
                Log.i(TAG, "Connecting to: $url")

                client!!.webSocket(url) {
                    session = this@webSocket
                    _connectionState.emit(ConnectionState.CONNECTED)
                    Log.i(TAG, "Connected to server")

                    // Receive messages loop
                    while (isActive) {
                        try {
                            val frame = incoming.receive()
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                Log.d(TAG, "Received: $text")
                                handleIncomingMessage(text)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error receiving message", e)
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                _connectionState.emit(ConnectionState.DISCONNECTED)
                session = null
                client?.close()
                client = null
            }
        }
    }

    private suspend fun handleIncomingMessage(text: String) {
        try {
            // Try parsing as JSON first
            val json = try {
                gson.fromJson(text, JsonObject::class.java)
            } catch (e: Exception) {
                // If JSON fails, try converting EDN to JSON
                // EDN format: {:event :alert, :data {:message "Hello"}}
                // Convert to: {"event":"alert","data":{"message":"Hello"}}
                val jsonText = convertEdnToJson(text)
                Log.d(TAG, "Converted EDN to JSON: $jsonText")
                gson.fromJson(jsonText, JsonObject::class.java)
            }
            _messages.emit(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: $text", e)
        }
    }
    
    /**
     * Convert Clojure EDN format to JSON.
     * Handles common patterns like :keywords and basic data structures.
     */
    private fun convertEdnToJson(edn: String): String {
        var result = edn
        // Replace :keyword with "keyword" (but not inside strings)
        // This is a simple approach that works for most cases
        result = result.replace(Regex(":([a-zA-Z][a-zA-Z0-9_-]*)")) { match ->
            "\"${match.groupValues[1]}\""
        }
        // Replace , with nothing (EDN uses optional commas)
        // JSON requires commas between elements, but EDN keywords replacement handles structure
        return result
    }

    suspend fun sendMessage(type: String, data: Map<String, Any?> = emptyMap()) {
        session?.let {
            try {
                val message = JsonObject().apply {
                    addProperty("type", type)
                    if (data.isNotEmpty()) {
                        val dataObj = JsonObject()
                        data.forEach { (k, v) ->
                            when (v) {
                                is String -> dataObj.addProperty(k, v)
                                is Number -> dataObj.addProperty(k, v)
                                is Boolean -> dataObj.addProperty(k, v)
                                null -> dataObj.add(k, null)
                            }
                        }
                        add("data", dataObj)
                    }
                }
                val json = gson.toJson(message)
                Log.d(TAG, "Sending: $json")
                it.outgoing.send(Frame.Text(json))
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
            }
        } ?: run {
            Log.e(TAG, "No active session to send message")
        }
    }

    fun disconnect() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                session?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing session", e)
            }
            session = null
            client?.close()
            client = null
            _connectionState.emit(ConnectionState.DISCONNECTED)
        }
    }
}

