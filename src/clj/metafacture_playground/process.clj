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
   "|fix(\""
   (content->tempfile-path fix ".fix")
   "\")|"))

(defn- morph->flux-content [morph]
  (str
   "|morph(\""
   (content->tempfile-path morph ".morph")
   "\")|"))

(defn- flux->flux-content [flux fix morph output]
  (-> flux
      (clj-str/replace #"PG_DATA(\s*|\n*)\|" "")
      (clj-str/replace #"\|(\s*|\n*)fix(\s*|\n*)\|" fix)
      (clj-str/replace #"\|(\s*|\n*)morph(\s*|\n*)\|" morph)
      (clj-str/replace #"\|(\s*|\n*)write\(\".*\"\)(\s*|\n*);" output)
      (clj-str/replace #"\|(\s*|\n*)print(\s*|\n*);" output)))

(defn- flux-output []
  (let [temp-file-path (content->tempfile-path "" ".txt")]
    [temp-file-path
     (str
      "|write(\""
      temp-file-path
      "\");")]))

(defn- ->flux-content [data flux fix morph]
  (let [fix (fix->flux-content fix)
        morph (morph->flux-content morph)
        [out-path output] (flux-output)
        data-via-playground? (re-find #"PG_DATA" flux)]
    [out-path
     (str
      (when data-via-playground? (data->flux-content data))
      (flux->flux-content flux fix morph output))]))

(defn- process-flux [file-path out-path]
  (Flux/main (into-array [file-path]))
  (println (str "Processed flux file: " file-path))
  (slurp out-path))

(defn process [data flux fix morph]
  (let [[out-path flux-content] (->flux-content data flux fix morph)]
    (-> flux-content
        (content->tempfile-path ".flux")
        (process-flux out-path))))
