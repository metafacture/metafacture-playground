(ns metafacture-playground.server
  (:require [metafacture-playground.handler :refer [handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

 (defn -main [& _args]
   (let [port (or (env :port) 3000)]
     (println "Start server with port " port)
     (run-jetty handler {:port port :join? false})))
