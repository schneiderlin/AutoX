# WebSocket Implementation Plan for AutoX

## Goal
Set up WebSocket communication between Android client and Clojure server for testing and development.

## Requirements
1. Client button press sends WebSocket message to server
2. Server logs received messages
3. Server can send messages via REPL for testing
4. Simple JSON-based message format

## Current State Analysis

### Server (script_server/)
- Uses `taoensso.sente` for WebSocket via jetty-main library
- Sente WebSocket currently disabled (`ws-server: nil`)
- HTTP endpoints: `/api/query`, `/api/command`
- Port: 3000 (from env var or default)

### Client (app/)
- Has Ktor WebSocket implementation (`KtorWebSocket.kt`, `DevPlugin.kt`)
- Uses JSON messages with Gson
- Existing WebSocket server on port 9317 for USB debugging
- Existing WebSocket client for remote connections

## Implementation Plan

### Phase 1: Server Side (Clojure)

#### 1.1 Add Dependencies
**File:** `script_server/deps.edn`
- Add `ring-websocket` dependency

```clojure
{:paths ["src"]
 :deps {linzihao/jetty-main {:git/url "https://github.com/schneiderlin/synapse.git"
                             :git/sha "82580b5f6f0b5fd0d0b2ed5fdadd6f4d567b938d"
                             :deps/root "components/jetty-main"}
        ring/ring-websocket {:mvn/version "1.12.1"}}}
```

#### 1.2 Create WebSocket Handler
**File:** `script_server/src/script_server/ws.clj` (new file)

```clojure
(ns script-server.ws
  (:require [ring.websocket :as ws]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.data.json :as json]))

;; Socket registry - maps username/id to socket
(defonce !sockets (atom {}))

;; WebSocket Handler
(defn ws-handler [request]
  (assert (ws/upgrade-request? request))
  {::ws/listener
   {:on-open
    (fn [socket]
      ;; Extract client id from query string: /ws?client_id=someid
      (let [query-string (:query-string request)
            client-id (when query-string
                        (-> (str/split query-string #"=")
                            second))]
        (println "WebSocket connected:" client-id)
        ;; Register the socket
        (swap! !sockets assoc (or client-id "unknown") socket)
        ;; Send confirmation to client
        (ws/send socket "{\"type\":\"connected\",\"message\":\"WebSocket connection established\"}")))

    :on-message
    (fn [socket message]
      (println "Server received:" message)
      ;; Parse JSON message
      (try
        (let [payload (json/read-str message :key-fn keyword)]
          ;; Handle different message types
          (case (:type payload)
            "ping"
            (ws/send socket "{\"type\":\"pong\",\"message\":\"pong\"}")

            "test"
            (ws/send socket "{\"type\":\"test_response\",\"message\":\"Server received test message\"}")

            ;; Default: echo back
            (ws/send socket (json/write-str {:type "echo" :data payload}))))
        (catch Exception e
          (println "Error parsing message:" e)
          (ws/send socket "{\"type\":\"error\",\"message\":\"Invalid message format\"}")))))

    :on-close
    (fn [socket status-code reason]
      (println "WebSocket closed:" status-code reason)
      ;; Remove socket from registry
      (swap! !sockets #(into {} (filter (fn [[k v]] (not (= v socket))) %))))}})

;; REPL Helper: Send message to specific client
(defn send-to-client [client-id message]
  (when-let [socket (get @!sockets client-id)]
    (ws/send socket message)
    (println "Sent to" client-id ":" message))
  (when (nil? (get @!sockets client-id))
    (println "No client found with id:" client-id)))

;; REPL Helper: Broadcast to all clients
(defn broadcast [message]
  (doseq [[client-id socket] @!sockets]
    (try
      (ws/send socket message)
      (println "Broadcast to" client-id ":" message)
      (catch Exception e
        (println "Broadcast failed for" client-id ":" e)))))

;; REPL Helper: List connected clients
(defn list-clients []
  (keys @!sockets))
```

#### 1.3 Add WebSocket Route
**File:** `script_server/src/script_server/api.clj`

Add WebSocket route to the config:
```clojure
(def config
  {:ws-server {:handler ws-handler}  ;; New WebSocket server
   :jetty/routes {:ws-server (ig/ref :ws-server)}
   :jetty/handler (ig/ref :jetty/routes)
   :adapter/jetty {:port (Integer. (or (System/getenv "PORT") "3000"))
                   :handler (ig/ref :jetty/handler)}})

;; Add init-key for ws-server
(defmethod ig/init-key :ws-server [_ {:keys [handler]}]
  (println "WebSocket server configured")
  {:handler handler})

;; Update routes to include WebSocket endpoint
(defmethod ig/init-key :jetty/routes [_ {:keys [ws-server] :as system}]
  (jm/make-routes system (:handler ws-server) query-handler command-handler))
```

Also need to update `jetty-main/core.clj` to support the custom WebSocket handler. We'll need to either:
- Option A: Modify jetty-main to support raw WebSocket handlers
- Option B: Use a simpler standalone WebSocket setup

Let me check the jetty-main library structure and propose the cleanest approach.

### Phase 2: Client Side (Android)

#### 2.1 Add WebSocket Service
**File:** `app/src/main/java/org/autojs/autojs/devplugin/ServerWebSocket.kt` (new file)

```kotlin
package org.autojs.autojs.devplugin

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.websocket.Frame
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
    private var currentClientId: String? = null

    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState = _connectionState.asSharedFlow()

    private val _messages = MutableSharedFlow<JsonObject>()
    val messages = _messages.asSharedFlow()

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    suspend fun connect(
        serverUrl: String = DEFAULT_SERVER_URL,
        clientId: String = "android_client"
    ) {
        withContext(Dispatchers.IO) {
            _connectionState.emit(ConnectionState.CONNECTING)
            currentClientId = clientId

            try {
                client = HttpClient(OkHttp) {
                    install(HttpTimeout) {
                        connectTimeoutMillis = 10000
                        socketTimeoutMillis = 10000
                    }
                    install(WebSockets) {
                        pingInterval = 15000
                        maxFrameSize = Long.MAX_VALUE
                    }
                }

                val url = "$serverUrl?client_id=$clientId"
                client!!.webSocket(url) {
                    _connectionState.emit(ConnectionState.CONNECTED)
                    Log.i(TAG, "Connected to server: $url")

                    // Receive messages loop
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            Log.d(TAG, "Received: $text")
                            handleIncomingMessage(text)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                _connectionState.emit(ConnectionState.DISCONNECTED)
                client?.close()
            }
        }
    }

    private suspend fun handleIncomingMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            _messages.emit(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }

    suspend fun sendMessage(type: String, data: Map<String, Any?> = emptyMap()) {
        client?.let {
            val message = JsonObject().apply {
                addProperty("type", type)
                if (data.isNotEmpty()) {
                    // Add data properties
                    data.forEach { (k, v) ->
                        when (v) {
                            is String -> addProperty(k, v)
                            is Number -> addProperty(k, v)
                            is Boolean -> addProperty(k, v)
                            null -> addProperty(k, (String)null)
                        }
                    }
                }
            }
            val json = gson.toJson(message)
            Log.d(TAG, "Sending: $json")
            // Note: Need to send through the WebSocket session
            // This requires storing the session reference
        }
    }

    fun disconnect() {
        client?.close()
        client = null
    }
}
```

#### 2.2 Add Test Button UI
**File:** Create or modify existing UI to add test button

Options:
- Add to existing DevPlugin settings UI
- Create a simple test Activity/Fragment
- Add to main app UI temporarily

#### 2.3 Log Messages
Add logging for received messages to console and/or Android logcat

## Message Protocol

### Client → Server
```json
{
  "type": "test",
  "data": {
    "message": "Hello from Android"
  }
}
```

### Server → Client
```json
{
  "type": "test_response",
  "message": "Server received test message"
}
```

## REPL Usage Examples

```clojure
;; In server REPL after starting

;; List connected clients
(script-server.ws/list-clients)
;; => ("android_client")

;; Send message to specific client
(script-server.ws/send-to-client "android_client" "{\"type\":\"alert\",\"message\":\"Hello from server\"}")

;; Broadcast to all clients
(script-server.ws/broadcast "{\"type\":\"broadcast\",\"message\":\"Server rebooting in 5 minutes\"}")

;; Send custom command
(script-server.ws/send-to-client "android_client" "{\"type\":\"command\",\"action\":\"run_script\",\"script\":\"test.js\"}")
```

## Testing Checklist

- [ ] Server starts without errors
- [ ] WebSocket route accessible at `/ws?client_id=test`
- [ ] Client can connect to server
- [ ] Client button sends message
- [ ] Server logs received messages
- [ ] Server REPL can send messages to client
- [ ] Client logs received server messages
- [ ] Connection handles disconnect/reconnect

## Open Questions

1. Should we integrate with existing jetty-main WebSocket (Sente) or create separate handler?
   - Recommendation: Create separate Ring WebSocket handler for simplicity

2. Should we use JSON or EDN for messages?
   - Recommendation: JSON (matches existing Android client pattern)

3. Where to add the test button in the Android UI?
   - Need to explore existing UI structure
