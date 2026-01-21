(ns script-server.ws
  "WebSocket handler for Android client communication.
   Uses the unified WebSocket API from jetty-main.")

(defn ws-handler
  "WebSocket business handler using unified API.
   Receives ws-ctx and msg with {:event :data :client-id :reply!}"
  [ws-ctx msg]
  (let [{:keys [event data client-id reply!]} msg]
    (println "WebSocket event:" event "from:" client-id "data:" data)
    
    (case event
      ;; Connection event
      :ws/open
      (do
        (println "Client connected:" client-id)
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
      (println "Client disconnected:" client-id)
      
      ;; Default: echo back
      (do
        (println "Unknown event, echoing back:" event)
        (reply! {:type "echo" :event (name event) :data data})))))


