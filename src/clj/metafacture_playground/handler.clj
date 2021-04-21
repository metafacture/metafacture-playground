(ns metafacture-playground.handler
  (:require
   [compojure.core :refer [GET defroutes]]
   [compojure.route :refer [resources not-found]]
   [metafacture-playground.process :refer [process]]
   [ring.util.response :refer [resource-response header response]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.params :refer [wrap-params]]))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/process" [data flux fix]
    (-> (process data flux fix)
        (response)
        (header "Access-Control-Allow-Origin" "*")))
  (resources "/")
  (not-found "Page not found"))

(def dev-handler (-> routes wrap-params wrap-reload))

(def handler (wrap-params routes))
