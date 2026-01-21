## 1. Android Client Implementation
- [ ] 1.1 Add `:get-layout` message handler in `ServerWebSocket.kt`
- [ ] 1.2 Integrate `LayoutInspector.captureCurrentWindow()` on receive of `:get-layout`
- [ ] 1.3 Serialize `NodeInfo` tree to JSON for WebSocket response
- [ ] 1.4 Send response with type `:layout` containing layout data
- [ ] 1.5 Add error handling when accessibility service is disabled
- [ ] 1.6 Update `WebSocketTestActivity.kt` UI to display incoming layout messages

## 2. Clojure Server Implementation
- [ ] 2.1 Add `:layout` case in `ws-handler` to receive/process layout data from client
- [ ] 2.2 Add helper function `(send-get-layout client-id)` to send command to specific client
- [ ] 2.3 Add optional: save received layouts to `layouts/` directory with timestamp

## 3. Testing
- [ ] 3.1 Test Android app responds to `:get-layout` with valid JSON
- [ ] 3.2 Test error case when accessibility service is disabled
- [ ] 3.3 Test Clojure server receives and parses layout data
- [ ] 3.4 Test end-to-end: server command → app → layout response
