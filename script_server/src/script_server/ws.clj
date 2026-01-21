(ns script-server.ws
  "WebSocket handler for Android client communication.
   Uses the unified WebSocket API from jetty-main.")

;; Keep reference to adapter for sending messages from REPL
(defonce !ws-adapter (atom nil))

(defn set-adapter! [adapter]
  (reset! !ws-adapter adapter))

;; REPL helper functions

(defn list-clients
  "List all connected WebSocket clients."
  []
  (if @!ws-adapter
    @(:clients @!ws-adapter)
    []))

(defn send-to-client
  "Send a message to a specific client by client-id.
   Example: (send-to-client \"android_test\" :notification {:message \"Hello!\"})"
  [client-id event-type data]
  (when @!ws-adapter
    (let [clients (-> @!ws-adapter
                      :clients
                      deref
                      :any)
          client (get clients client-id)]
      (if client
        (do
          (println "Sending to" client-id ":" event-type data)
          (:send! client {:event event-type :data data}))
        (println "Client not found:" client-id)))))

(defn broadcast
  "Broadcast a message to all connected clients.
   Example: (broadcast :notification {:message \"Hello all!\"})"
  [event-type data]
  (when @!ws-adapter
    (println "Broadcasting" event-type "to all clients")
    ((:broadcast! @!ws-adapter) event-type data)))

(comment
  (broadcast :notification {:message "Hello all!"})
  (broadcast :get-layout {})
  :rcf)

;; Layout-specific helpers

(defn send-get-layout
  "Request layout from a specific client.
   Client will respond with :layout event containing the UI hierarchy."
  [client-id]
  (send-to-client client-id :get-layout {}))

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
        (reply! {:event :connected
                 :message "WebSocket connection established"
                 :client-id client-id}))

      ;; Ping/pong for keep-alive
      :ping
      (reply! {:event :pong :timestamp (System/currentTimeMillis)})

      ;; Test message from client button press
      :test
      (do
        (println "Test message received from" client-id ":" data)
        (reply! {:event :test_response
                 :message "Server received test message"
                 :echo data}))

      ;; Layout response from client (after get-layout command)
      :layout
      (do
        (println "Layout received from" client-id)
        (println "Layout data:" data)
        ;; Optionally save to file
        ;; (spit (str "layouts/layout_" (System/currentTimeMillis) ".json")
        ;;       (:data data))
        ;; Acknowledge receipt
        (reply! {:event :layout_received
                 :timestamp (System/currentTimeMillis)}))

      ;; Run script command (placeholder for future)
      :run-script
      (do
        (println "Run script request:" data)
        (reply! {:event :script_queued :script (:script data)}))

      ;; Disconnection
      :ws/close
      (println "Client disconnected:" client-id)

      ;; Default: echo back
      (do
        (println "Unknown event, echoing back:" event)
        (reply! {:event :echo :original-event (name event) :data data})))))


(comment
  (list-clients) 

  (send-get-layout "android_test")
  :rcf)

