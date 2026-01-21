(ns script-server.api
  (:gen-class)
  (:require
   [ring.adapter.jetty :as jetty]
   [com.zihao.jetty-main.interface :as jm]
   [squint-compiler.api :as compiler]
   [script-server.ws :as ws]
   [clojure.core.async :as async]
   [integrant.core :as ig]))

(defn normalize-command [command]
  (if (string? (:command/kind command))
    (assoc command :command/kind (keyword (:command/kind command)))
    command))

(defn command-handler [system command]
  (println "command:" command)
  (let [command (normalize-command command)]
    (or (compiler/command-handler command))))

(defn normalize-query [query]
  (if (string? (:query/kind query))
    (assoc query :query/kind (keyword (:query/kind query)))
    query))

(defn query-handler [system query] 
  (let [query (normalize-query query)]
    (or (compiler/query-handler query))))

(comment
  (query-handler nil {:query/kind :query/scripts, :query/data {:page 1}})
  :rcf)

(def config
  {:ws/ws-server {:format :json}  ;; Will be initialized to Ring WS server
   :ws/ws-handler {:ws-server (ig/ref :ws/ws-server)}
   :jetty/routes {:ws-server (ig/ref :ws/ws-server)}
   :jetty/handler (ig/ref :jetty/routes)
   :adapter/jetty {:port (Integer. (or (System/getenv "PORT") "3000"))
                   :handler (ig/ref :jetty/handler)}})

;; Create Ring WebSocket server
(defmethod ig/init-key :ws/ws-server [_ config]
  (println "Creating Ring WebSocket server...")
  (jm/make-ring-ws-server config))

(defonce !ws-adapter (atom nil))

(comment
  ;; Check connected clients
  @(:clients @!ws-adapter)
  
  ;; Broadcast a message from REPL
  ((:broadcast! @!ws-adapter) :server/announcement {:msg "Hello from REPL!"}) 
  :rcf)

;; Start WebSocket handler
(defmethod ig/init-key :ws/ws-handler [_ {:keys [ws-server]}]
  (when ws-server
    (println "Starting WebSocket handler...")
    (let [stop-ch (async/chan)
          adapter (jm/ws-adapter ws-server)
          _ (ws/set-adapter! adapter)  ;; Store adapter for REPL access
          _ (reset! !ws-adapter adapter)
          handler (jm/make-unified-ws-handler ws/ws-handler)]
      (handler stop-ch adapter)
      stop-ch)))

;; Cleanup WebSocket handler on halt
(defmethod ig/halt-key! :ws/ws-handler [_ stop-ch]
  (when stop-ch
    (async/close! stop-ch)))

(defmethod ig/init-key :jetty/routes [_ {:keys [ws-server] :as system}]
  (jm/make-routes system ws-server query-handler command-handler))

(defmethod ig/init-key :jetty/handler [_ routes]
  (jm/make-handler routes :public-dir "public"))

(defmethod ig/init-key :adapter/jetty [_ {:keys [port handler]}]
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defn -main [& _]
  (ig/init config))

(comment
  (def system (-main))

  (def python-env (:cljpy/python-env system))
  :rcf)
