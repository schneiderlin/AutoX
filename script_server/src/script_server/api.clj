(ns script-server.api
  (:gen-class)
  (:require
   [ring.adapter.jetty :as jetty]
   [com.zihao.jetty-main.interface :as jm] 
   [squint-compiler.api :as compiler] 
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
  {:jetty/routes {:ws-server nil}
   :jetty/handler (ig/ref :jetty/routes)
   :adapter/jetty {:port (Integer. (or (System/getenv "PORT") "3000"))
                   :handler (ig/ref :jetty/handler)}})

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
