---
date: 2026-01-22T06:41:48Z
researcher: researcher
git_commit: 83b5ffa5d8bd0b55157a792844fb01d7b137b897
branch: setup-v7
repository: AutoX
topic: "AutoCLJS Script Fetching and WebSocket Communication"
tags: [research, codebase, autocljs, script-server, websocket]
status: complete
last_updated: 2026-01-22
last_updated_by: researcher
---

# Research: AutoCLJS Script Fetching and WebSocket Communication

**Date**: 2026-01-22T06:41:48Z
**Researcher**: researcher
**Git Commit**: 83b5ffa5d8bd0b55157a792844fb01d7b137b897
**Branch**: setup-v7
**Repository**: AutoX

## Research Question
How do AutoCLJS clients get JavaScript scripts from the script_server, and how do server and client communicate via WebSocket?

## Summary
AutoCLJS (autocljs) uses a two-part architecture:
1. **HTTP/REST API** - Clients fetch JavaScript scripts from the server using query endpoints
2. **WebSocket Connection** - Real-time bidirectional communication for commands, responses, and automation data

The script_server (Clojure) provides script metadata via `:query/scripts` endpoint, and the Android client (Kotlin) uses NodeScriptEngine to execute ES modules. WebSocket handles dynamic interactions like layout requests, script execution control, and status updates.

## Detailed Findings

### Server-Side Architecture (script_server)

#### 1. Main Server Entry Point
- **File**: `script_server/scripts-server.js` (Node.js development server)
  - Provides sample scripts at `/scripts` endpoint (lines 121-123)
  - Simple HTTP server for testing without full Clojure stack
  - Includes test scripts for console logging, automation, and module imports

#### 2. Clojure Server Core (api.clj)
- **File**: `script_server/src/script_server/api.clj`
  - Jetty-based HTTP server on port 3000 (line 39)
  - Integrates WebSocket and HTTP routes using jetty-main library
  - **Query Handler** (lines 26-28): Processes `:query/*` requests (e.g., `:query/scripts`)
  - **Command Handler** (lines 16-19): Processes `:command/*` requests (e.g., `:command/save-layout`)
  - Configuration at lines 34-40 shows system components

#### 3. Script Query Handler (squint_compiler/api.clj)
- **File**: `script_server/src/squint_compiler/api.clj`
  - Query handler for `:query/scripts` endpoint (lines 6-74)
  - Returns array of script objects with `name` and `code` fields
  - Provides 4 test scripts:
    1. Phase 1 - Console Test
    2. Phase 3 - Click Button Test
    3. Phase 4 - Common Modules Test
    4. Phase 5 - Squint-CLJS Test
  - Command handler for `:command/save-layout` (lines 82-90)
    - Saves layout data to JSON files in `layouts/` directory

#### 4. WebSocket Handler (ws.clj)
- **File**: `script_server/src/script_server/ws.clj`
  - WebSocket event handler for real-time communication (lines 57-111)
  - **Supported Events**:
    - `:ws/open` - Client connection established (lines 66-71)
    - `:ping` - Keep-alive heartbeat (lines 74-75)
    - `:test` - Test message from client (lines 78-83)
    - `:layout` - Layout data from client (lines 86-95)
    - `:run-script` - Script execution request (lines 98-101)
    - `:ws/close` - Client disconnection (lines 104-105)
  - REPL helper functions:
    - `send-to-client` (lines 20-34) - Send message to specific client
    - `broadcast` (lines 36-42) - Broadcast to all clients
    - `send-get-layout` (lines 51-55) - Request layout from client

### Client-Side Architecture (app1 - Android)

#### 1. WebSocket Client (ServerWebSocket.kt)
- **File**: `app1/src/main/java/com/autocljs/testapp/ServerWebSocket.kt`
  - Ktor-based WebSocket client using OkHttp engine
  - Default server URL: `ws://localhost:3000/ws` (line 31)
  - **Connection Flow** (lines 52-103):
    1. Creates HttpClient with WebSocket plugin
    2. Connects to server with client-id query parameter
    3. Enters receive loop to listen for messages
  - **Message Handling** (lines 105-130):
    - Parses JSON messages
    - Handles Clojure EDN format conversion (lines 114-116)
    - Routes server commands to appropriate handlers
  - **Server Commands** (lines 132-155):
    - `get-layout` - Requests current UI layout from client (lines 134, 139-155)
    - Sends layout response back via `layout` event
  - **Send Messages** (lines 173-200):
    - Constructs JSON messages with `event` and `data` fields
    - Supports sending test messages, pings, and custom events

#### 2. Test Activity (WebSocketTestActivity.kt)
- **File**: `app1/src/main/java/com/autocljs/testapp/WebSocketTestActivity.kt`
  - UI for testing WebSocket connection
  - Uses Compose UI with connection toggle
  - Displays message log of all WebSocket traffic
  - Can send test messages and ping requests
  - Default URL for emulator: `ws://10.0.2.2:3000/ws` (line 64)
    - 10.0.2.2 is Android emulator's host loopback

#### 3. Script Execution Engine (NodeScriptEngine.kt)
- **File**: `app1/src/main/java/com/autocljs/engine/NodeScriptEngine.kt`
  - NodeScriptEngine using Javet/V8 runtime
  - Executes ES modules (.mjs files) and JavaScript
  - **Execution Flow** (lines 151-212):
    1. Creates temporary file with script source
    2. Initializes module with resolver
    3. Handles async operations via PromiseListener
    4. Waits for completion
  - **Module Resolver** (lines 90-91):
    - Uses `SimpleNodeModuleResolver` for ES module loading
    - Supports Node.js-style module resolution

#### 4. Module Resolver (SimpleNodeModuleResolver.kt)
- **File**: `app1/src/main/java/com/autocljs/engine/SimpleNodeModuleResolver.kt`
  - Resolves ES modules from multiple sources:
    1. **Pre-bundled assets** - `modules/` directory (Node.js-style structure)
       - Package name: `"utils"` → `modules/utils/index.mjs`
       - Package path: `"squint-runtime/core.js"` → `modules/squint-runtime/core.js`
    2. **Module directory** - `context.filesDir/v7_modules`
    3. **Relative paths** - From referrer module
  - Supports ES module compilation from source code (lines 70-74)
  - Caches compiled modules for performance (lines 299-301)

#### 5. Script Service (ScriptEngineService.kt)
- **File**: `app1/src/main/java/com/autocljs/ScriptEngineService.kt`
  - Factory for script engines
  - Supports multiple engines:
    - **Rhino** (ClojureScriptEngine) - Backward compatibility
    - **Node.js** (NodeScriptEngine) - ES modules
  - **Execution Flow** (lines 67-77):
    1. Selects appropriate engine based on script source
    2. Executes script
    3. Returns ScriptExecution result

#### 6. Script Sources
- **File**: `app1/src/main/java/com/autocljs/script/NodeScriptSource.kt`
  - Holds ES module script source
  - Engine name: `com.autocljs.engine.NodeScriptEngine`

### Communication Flow

#### 1. Script Fetching Process
1. Client makes HTTP query to server: `{:query/kind :query/scripts, :query/data {:page 1}}`
2. Server responds with script array: `[{:name "...", :code "..."}]`
3. Client creates `NodeScriptSource` with script code
4. Client executes script via `ScriptEngineService`
5. Script runs in Node.js/V8 runtime with access to:
   - `console` - Logging API
   - `auto` - Automation API (click, longClick, etc.)
   - `layout` - UI hierarchy API

#### 2. WebSocket Communication Flow

**Connection Establishment**:
```
Android Client                          Script Server (Clojure)
     |                                           |
     |---[WebSocket connect]------------------->|
     |    URL: ws://host:3000/ws               |
     |    Query: client-id=android_test         |
     |<--[:ws/open response]--------------------|
     |    {:event :connected, ...}              |
```

**Layout Request**:
```
Server                                   Android Client
  |                                          |
  |---[broadcast :get-layout]---------------->|
  |                                          |
  |<---[:layout response]---------------------|
  |     {:event :layout, :data {...}}        |
```

**Test Message**:
```
Android Client                          Script Server
     |                                           |
     |---[:test {:message "Hello"}]-------------->|
     |                                           |
     |<--[:test_response {:echo {...}}]----------|
```

### Protocol Details

#### Message Format
**Server → Client**:
```json
{
  "event": "event_name",
  "data": {
    "field1": "value1",
    "field2": "value2"
  }
}
```

**Client → Server**:
```json
{
  "event": "event_name",
  "data": {
    "message": "test data",
    "client-id": "android_test"
  }
}
```

#### Event Types
**From Server**:
- `:connected` - Connection acknowledgment
- `:get-layout` - Request UI hierarchy
- `:notification` - Broadcast message
- `:layout_received` - Acknowledge layout receipt
- `:script_queued` - Script execution queued

**From Client**:
- `:test` - Test message
- `:ping` - Keep-alive ping
- `:layout` - Layout data response
- `:run-script` - Request to run script
- `:ws/close` - Disconnect notification

## Code References

### Server-Side
- `script_server/src/script_server/api.clj:75` - Jetty routes setup with query/command handlers
- `script_server/src/script_server/ws.clj:57-111` - WebSocket event handler implementation
- `script_server/src/squint_compiler/api.clj:6-74` - Script query handler returning scripts list
- `script_server/scripts-server.js:121-123` - Simple Node.js HTTP endpoint for testing

### Client-Side
- `app1/src/main/java/com/autocljs/testapp/ServerWebSocket.kt:52-103` - WebSocket connection establishment
- `app1/src/main/java/com/autocljs/testapp/ServerWebSocket.kt:132-155` - Server command handling
- `app1/src/main/java/com/autocljs/engine/NodeScriptEngine.kt:151-212` - Script execution with V8 runtime
- `app1/src/main/java/com/autocljs/engine/SimpleNodeModuleResolver.kt:80-141` - Module resolution logic
- `app1/src/main/java/com/autocljs/ScriptEngineService.kt:67-77` - Script execution orchestration

## Architecture Documentation

### Two-Tier Architecture
1. **HTTP Layer** - Script retrieval and metadata
   - GET `/scripts` - Fetch script list
   - POST `:query/scripts` - Query scripts with parameters
   - Static asset serving from `public/` directory

2. **WebSocket Layer** - Real-time communication
   - Persistent connection for bidirectional messages
   - Event-driven protocol
   - Support for multiple concurrent clients

### Script Execution Pipeline
```
Server (Clojure)          Android Client
      |                          |
      |  HTTP Query              |
      |<-------------------------|  :query/scripts
      |                          |
      |  Script List             |
      |------------------------->|  [{:name, :code}]
      |                          |
      |  [Create NodeScriptSource]
      |                          |
      |  [Execute via NodeScriptEngine]
      |                          |  -> V8 Runtime
      |                          |  -> Module Resolution
      |                          |  -> Script Execution
      |                          |
      |  [Console Output]         |
      |<-------------------------|  :layout (or other)
```

### Module Loading Strategy
The client uses a sophisticated module resolution strategy:
1. **Priority Order**:
   - Pre-bundled assets (`modules/` in APK assets)
   - Module directory (`filesDir/v7_modules`)
   - Relative imports from referrer
2. **Node.js-style resolution**:
   - Package imports resolve to `index.mjs`
   - Supports both `.mjs` and `.js` extensions
   - Relative path support with `../` and `./`

## Historical Context
- AutoCLJS is a port of AutoX to Android with JavaScript execution
- Uses Javet library for V8/Node.js runtime on Android
- WebSocket protocol enables remote control and debugging
- Layout inspection via AccessibilityService API

## Related Research
- See `script_server/layouts/` directory for layout snapshots received via WebSocket
- Test activity `WebSocketTestActivity.kt` provides UI for protocol debugging

## Open Questions
1. **HTTP Script Fetching**: The code shows query handlers but actual HTTP client implementation for fetching scripts from server wasn't found in app1. May be using built-in HttpURLConnection or Ktor HTTP client not yet visible in code.
2. **Production vs Development**: `scripts-server.js` is for development/testing, while Clojure server (`api.clj`) is the production implementation.
3. **Security**: WebSocket has no authentication (client-id is just a string parameter).
