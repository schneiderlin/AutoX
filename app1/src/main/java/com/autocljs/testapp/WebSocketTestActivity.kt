package com.autocljs.testapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Test Activity for WebSocket connection to Clojure server.
 * 
 * Usage:
 * 1. Start the Clojure server: cd script_server && clj -M -m script-server.api
 * 2. Launch this activity
 * 3. Enter server URL (use 10.0.2.2 for emulator to reach host localhost)
 * 4. Toggle connection switch
 * 5. Send test messages and observe server logs
 * 
 * Server REPL commands:
 * (script-server.ws/list-clients) - List connected clients
 * (script-server.ws/send-to-client "android_test" :alert {:message "Hello!"}) - Send to client
 * (script-server.ws/broadcast :notification {:message "Hello all!"}) - Broadcast
 */
class WebSocketTestActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "WebSocketTest"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WebSocketTestScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSocketTestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // WebSocket instance
    val webSocket = remember { ServerWebSocket() }
    
    // UI State
    var serverUrl by remember { mutableStateOf("ws://10.0.2.2:3000/ws") }
    var clientId by remember { mutableStateOf("android_test") }
    var connected by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Not connected") }
    var messageLog by remember { mutableStateOf("") }
    var testMessage by remember { mutableStateOf("Hello from Android") }
    
    // Collect connection state
    LaunchedEffect(Unit) {
        webSocket.connectionState.collect { state ->
            connected = state == ServerWebSocket.ConnectionState.CONNECTED
            statusText = when (state) {
                ServerWebSocket.ConnectionState.CONNECTED -> "✓ Connected"
                ServerWebSocket.ConnectionState.CONNECTING -> "⏳ Connecting..."
                ServerWebSocket.ConnectionState.DISCONNECTED -> "✗ Disconnected"
            }
        }
    }
    
    // Collect messages
    LaunchedEffect(Unit) {
        webSocket.messages.collect { message ->
            Log.d("WebSocketTest", "Received: $message")
            val msgType = message.get("type")?.asString ?: "unknown"
            val msgContent = message.get("message")?.asString 
                ?: message.get("data")?.toString() 
                ?: message.toString()
            val logEntry = "[$msgType] $msgContent\n"
            messageLog = logEntry + messageLog
            Toast.makeText(context, "Server: $msgType", Toast.LENGTH_SHORT).show()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "WebSocket Test",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Server URL input
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !connected,
            singleLine = true
        )
        
        // Client ID input
        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it },
            label = { Text("Client ID") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !connected,
            singleLine = true
        )
        
        // Connection status and toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusText,
                color = when {
                    connected -> Color(0xFF4CAF50)
                    statusText.contains("Connecting") -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
            )
            Switch(
                checked = connected,
                onCheckedChange = { enable ->
                    scope.launch {
                        if (enable) {
                            try {
                                webSocket.connect(serverUrl, clientId)
                            } catch (e: Exception) {
                                Log.e("WebSocketTest", "Connection error", e)
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            webSocket.disconnect()
                        }
                    }
                }
            )
        }
        
        HorizontalDivider()
        
        // Send message section
        Text(
            text = "Send Message",
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = testMessage,
            onValueChange = { testMessage = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth(),
            enabled = connected,
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        webSocket.sendMessage("test", mapOf("message" to testMessage))
                        messageLog = "[SENT] test: $testMessage\n" + messageLog
                    }
                },
                enabled = connected,
                modifier = Modifier.weight(1f)
            ) {
                Text("Send Test")
            }
            
            Button(
                onClick = {
                    scope.launch {
                        webSocket.sendMessage("ping")
                        messageLog = "[SENT] ping\n" + messageLog
                    }
                },
                enabled = connected,
                modifier = Modifier.weight(1f)
            ) {
                Text("Ping")
            }
        }
        
        HorizontalDivider()
        
        // Message log
        Text(
            text = "Message Log",
            style = MaterialTheme.typography.titleMedium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { messageLog = "" }) {
                Text("Clear")
            }
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = messageLog.ifEmpty { "No messages yet..." },
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Instructions
        Text(
            text = "Tip: Use 10.0.2.2 for emulator to reach host localhost",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

