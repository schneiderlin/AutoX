(ns script-server.ws
  "WebSocket handler for Android client communication.
   Uses the unified WebSocket API from jetty-main.")

;; Holds the ws-ctx for REPL access
(defonce !ws-ctx (atom nil))

;; Socket registry - maps client-id to connection info
(defonce !clients (atom {}))

(defn ws-handler
  "WebSocket business handler using unified API.
   Receives ws-ctx and msg with {:event :data :client-id :reply!}"
  [ws-ctx msg]
  (let [{:keys [event data client-id reply!]} msg]
    ;; Store ws-ctx for REPL access (contains send!, broadcast!, clients)
    (reset! !ws-ctx ws-ctx)
    
    (println "WebSocket event:" event "from:" client-id "data:" data)
    
    (case event
      ;; Connection event
      :ws/open
      (do
        (println "Client connected:" client-id)
        (swap! !clients assoc client-id {:connected-at (System/currentTimeMillis)})
        (reply! {:type "connected" 
                 :message "WebSocket connection established"
                 :client-id client-id}))
      
      ;; Ping/pong for keep-alive
      :ping
      (reply! {:type "pong" :timestamp (System/currentTimeMillis)})
      
      ;; Test message from client button press
      :test
      (do
        (println "Test message received from" client-id ":" data)
        (reply! {:type "test_response" 
                 :message "Server received test message"
                 :echo data}))
      
      ;; Run script command (placeholder for future)
      :run-script
      (do
        (println "Run script request:" data)
        (reply! {:type "script_queued" :script (:script data)}))
      
      ;; Disconnection
      :ws/close
      (do
        (println "Client disconnected:" client-id)
        (swap! !clients dissoc client-id))
      
      ;; Default: echo back
      (do
        (println "Unknown event, echoing back:" event)
        (reply! {:type "echo" :event (name event) :data data})))))

;; ============================================
;; REPL Helper Functions
;; ============================================

(defn list-clients
  "List all connected client IDs."
  []
  (if-let [ctx @!ws-ctx]
    (let [clients-atom (:clients ctx)]
      (if clients-atom
        @clients-atom
        (keys @!clients)))
    (keys @!clients)))

(defn send-to-client
  "Send a message to a specific client.
   
   Usage:
   (send-to-client \"android_client\" :alert {:message \"Hello!\"})
   (send-to-client \"android_client\" :command {:action \"run_script\" :script \"test.js\"})"
  [client-id event data]
  (if-let [ctx @!ws-ctx]
    (let [send! (:send! ctx)]
      (send! client-id event data)
      (println "Sent to" client-id ":" event data))
    (println "No WebSocket context available. Is the server running?")))

(defn broadcast
  "Broadcast a message to all connected clients.
   
   Usage:
   (broadcast :notification {:message \"Server maintenance in 5 minutes\"})"
  [event data]
  (if-let [ctx @!ws-ctx]
    (let [broadcast! (:broadcast! ctx)]
      (broadcast! event data)
      (println "Broadcast:" event data))
    (println "No WebSocket context available. Is the server running?")))

(defn send-json
  "Send a raw JSON message to a specific client.
   Useful for testing with simple JSON strings.
   
   Usage:
   (send-json \"android_client\" {:type \"alert\" :message \"Hello from REPL\"})"
  [client-id json-map]
  (send-to-client client-id :message json-map))

(defn broadcast-json
  "Broadcast a raw JSON message to all clients.
   
   Usage:
   (broadcast-json {:type \"broadcast\" :message \"Hello everyone!\"})"
  [json-map]
  (broadcast :message json-map))

(comment
  ;; REPL usage examples:
  
  ;; List connected clients
  (list-clients)
  ;; => #{"android_test" "bob"}
  
  ;; Send message to specific client
  (send-to-client "android_test" :alert {:message "Hello from server REPL"})
  (send-to-client "bob" :alert {:message "Hello from server REPL"})
  
  ;; Broadcast to all clients
  (broadcast :notification {:message "Server rebooting in 5 minutes"})
  
  ;; Send command to run a script
  (send-to-client "android_test" :command {:action "run_script" :script "test.js"})
  
  ;; Simple JSON message
  (send-json "android_test" {:type "custom" :data {:foo "bar"}})
  
  :rcf)
