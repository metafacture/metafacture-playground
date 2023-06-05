(ns metafacture-playground.server
  (:require [metafacture-playground.handler :refer [handler]]
            [config.core :refer [env]]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty :refer [run-jetty]])
  (:import
   (java.lang SecurityManager))
  (:gen-class))
 
 (defn -main [& _args]
   (let [port (or (env :port) 3000)]
     (System/setSecurityManager (new SecurityManager))
     (log/info "Start server with port" port)
     (run-jetty #'handler {:port port :join? false :request-header-size 65536})))