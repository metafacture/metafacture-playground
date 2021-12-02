(ns metafacture-playground.handler
  (:require
   [compojure.core :refer [GET POST defroutes]]
   [compojure.route :refer [resources not-found]]
   [metafacture-playground.process :refer [process]]
   [ring.util.response :refer [resource-response header response]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.request :refer [body-string]]
   [clojure.data.json :as json]
   [clojure.stacktrace :as st]
   [clojure.java.io :as io]))

(defn wrap-body-string [handler]
  (fn [request]
    (let [body-str (body-string request)]
      (handler (assoc request :body body-str)))))

(defn exception-handler [exception uri]
  (let [json-body (json/write-str {:message (.getMessage exception)
                                   :stacktrace (with-out-str (st/print-cause-trace exception))
                                   :uri uri})]
       {:status 500
        :body json-body}))

(defn process-request [data flux fix morph uri]
  (try
    (-> (process data flux fix morph)
        (response)
        (header "Access-Control-Allow-Origin" "*"))
    (catch Exception e
      (exception-handler e uri))))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/process" [data flux fix morph uri]
    (process-request data flux fix morph uri))
  (GET "/examples" request
    (try
      (let [files (-> (io/file "resources/examples/")
                      file-seq
                      rest)
            files-content (reduce
                           (fn [result file]
                             (assoc result (.getName file) (slurp file)))
                           {}
                           files)]
        (response (json/write-str files-content)))
      (catch Exception e
        (exception-handler e (:uri request)))))
  (POST "/process" request
    (let [body (-> request :body (json/read-str :key-fn keyword))
          {:keys [data flux fix morph]} body]
      (process-request data flux fix morph (:uri request))))
  (resources "/")
  (not-found "Page not found"))

(def dev-handler (-> routes
                     wrap-params
                     wrap-body-string
                     wrap-reload))

(def handler (-> routes
                 wrap-params
                 wrap-body-string))
