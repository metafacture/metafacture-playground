(ns metafacture-playground.handler
  (:require
   [compojure.core :refer [GET defroutes]]
   [compojure.route :refer [resources not-found]]
   [metafacture-playground.process :refer [process]]
   [ring.util.response :refer [resource-response header response]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.params :refer [wrap-params]]
   [shadow.http.push-state :as push-state]))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/process" [data flux fix]
    (-> (process data flux fix)
        (response)
        (header "Access-Control-Allow-Origin" "*")))
  (resources "/")
  (not-found "Page not found"))

(def dev-handler (-> #'routes wrap-reload push-state/handle))

(def handler (wrap-params routes))
