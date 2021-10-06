(ns metafacture-playground.handler
  (:require
   [compojure.core :refer [GET defroutes]]
   [compojure.route :refer [resources not-found]]
   [metafacture-playground.process :refer [process]]
   [ring.util.response :refer [resource-response header response]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.params :refer [wrap-params]]
   [clojure.data.json :as json]
   [clojure.stacktrace :as st]))

(defn exception-handler [exception request]
  (let [json-body (json/write-str {:message (.getMessage exception)
                                    :stacktrace (with-out-str (st/print-stack-trace exception))
                                    :uri (:uri request)})]
       {:status 500
        :body json-body}))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/process" [data flux fix morph :as request]
    (try
      (-> (process data flux fix morph)
          (response)
          (header "Access-Control-Allow-Origin" "*"))
      (catch Exception e
        (exception-handler e request))))
  (resources "/")
  (not-found "Page not found"))

(def dev-handler (-> routes wrap-params wrap-reload))

(def handler (-> routes wrap-params))
