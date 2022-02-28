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
    (let [response (-> (process data flux fix morph)
                       (response)
                       (header "Access-Control-Allow-Origin" "*"))]
      (if-let [file-name (->> flux
                              (re-find #"\|\s*write\(.*\"(.*)\"\)")
                              second)]
          (header response "Content-Disposition" (str "attachment; filename=\"" file-name "\""))
        response))
    (catch Exception e
      (exception-handler e uri))))

(defn- files->content [entries]
  (reduce
   (fn [result item]
     (cond
       (.isFile item) (assoc result (.getName item) (slurp item))
       (.isDirectory item) (assoc result (.getName item) (files->content (.listFiles item)))
       :else result))
   {}
   entries))

(defn- versions-from-files []
  (let  [dependencies (-> "project.clj" slurp read-string (nth 6))]
    (->> (io/file "resources/versions/")
         .listFiles
         (filter #(.isFile %))
         files->content
         (reduce
          (fn [result [file-name file-content]]
            (if-let [matching-label (-> (filter #(= (-> % first name)
                                                    file-name)
                                                dependencies)
                                        first
                                        second)]
              (assoc result file-name {:version-label matching-label
                                       :link file-content})
              result))
          {}))))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/process" [data flux fix morph uri]
    (process-request data flux fix morph uri))
  (GET "/examples" request
    (try
      (->> (io/file "resources/examples/")
           .listFiles
           files->content
           json/write-str
           response)
      (catch Exception e
        (exception-handler e (:uri request)))))
  (GET "/versions" request
    (try
      (-> (versions-from-files)
          json/write-str
          response)
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
