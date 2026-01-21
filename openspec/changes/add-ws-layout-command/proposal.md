# Change: Add WebSocket Layout Command

## Why
Currently, the Android test app can only initiate layout uploads to the Clojure server via button press. For remote control and automation scenarios, the server needs to request the current layout on-demand to inspect the device UI state programmatically.

## What Changes
- Add `{:type "get-layout"}` handler on Android client (server→client command)
- Add `{:type "layout"}` response message with serialized NodeInfo tree (client→server)
- Update Clojure server to send `{:type "get-layout"}` command and handle `:layout` response
- Ensure layout capture requires accessibility service (error handling)

**Note on convention:** Server→Client uses `:type` (string), Client→Server uses `:event` (keyword)

## Impact
- **Affected specs:** `websocket-protocol`
- **Affected code:**
  - `app1/src/main/java/com/autocljs/testapp/ServerWebSocket.kt`
  - `app1/src/main/java/com/autocljs/testapp/WebSocketTestActivity.kt`
  - `script_server/src/script_server/ws.clj`
