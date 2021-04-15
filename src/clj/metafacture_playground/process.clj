(ns metafacture-playground.process
  (:require
   [clojure.java.io :as jio]
   [clojure.string :as clj-str])
  (:import
   (java.io File)
   (org.metafacture.runner Flux)))

(defn- content->tempfile-path [content file-extension]
  (let [temp-file (File/createTempFile "metafix" file-extension)]
   (with-open [file (jio/writer temp-file)]
     (binding [*out* file]
       (print content)))
    (.deleteOnExit temp-file)
    (clj-str/replace (.getAbsolutePath temp-file) "\\" "/")))

(defn- data->flux-content [data]
  (if data
    (str "\""
         (content->tempfile-path data ".txt")
         "\""
         "|open-file|")
    ""))

(defn- fix->flux-content [fix]
  (str
   "|org.metafacture.metamorph.Metafix(\""
   (content->tempfile-path fix ".fix")
   "\")|"))

(defn- flux->flux-content [flux fix]
  (-> flux
      (clj-str/replace "\n|" "|")
      (clj-str/replace "\\s?\\|\\s?" "|")
      (clj-str/replace "|fix|" fix)))

(defn- flux-output []
  (let [temp-file-path (content->tempfile-path "" ".txt")]
    [temp-file-path
     (str
      "|write(\""
      temp-file-path
      "\");")]))

(defn- ->flux-content [data flux fix]
  (let [fix (fix->flux-content fix)
        [out-path output] (flux-output)]
    [out-path
     (str
      (data->flux-content data)
      (flux->flux-content flux fix)
      output)]))

(defn- process-flux [file-path out-path]
  (try
    (Flux/main (into-array [file-path]))
    (println "Processed flux file.")
    (slurp out-path)
    (catch Exception e
      (println "Something went wrong: " (.getMessage e))
      (println "Exception: " (.printStackTrace e))
      (str "Sorry, something went wrong: " (.getMessage e)))
    (catch Error e
      (println "Something really went wrong: " (.getMessage e))
      (println "Error: " (.printStackTrace e))
      (str "Sorry, something went wrong: " (.getMessage e)))))

(defn process [data flux fix]
  (let [[out-path flux-content] (->flux-content data flux fix)]
       (-> flux-content
           (content->tempfile-path ".flux")
           (process-flux out-path))))
