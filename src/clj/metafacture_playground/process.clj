(ns metafacture-playground.process
  (:require
   [clojure.java.io :as jio]
   [clojure.string :as clj-str]
   [clojure.tools.logging :as log])
  (:import
   (java.io File)
   (org.metafacture.runner Flux)))

(defn- content->tempfile-path [content file-extension]
  (let [temp-file (File/createTempFile "metafix" file-extension)]
   (with-open [file (jio/writer temp-file)]
     (binding [*out* file]
       (print content)))
    (.deleteOnExit temp-file)
    (let [file-path (clj-str/replace (.getAbsolutePath temp-file) "\\" "/")]
      (log/info "Wrote content to temp file:" file-path)
      (log/trace "Content" content)
      file-path)))

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
        data-via-playground? (re-find #"PG_DATA" flux)
        flux-content (str
                      (when data-via-playground? (data->flux-content data))
                      (flux->flux-content flux fix morph output))]
    (log/info "Converted input data to flux content.")
    [out-path flux-content]))

(defn- process-flux [file-path out-path]
  (Flux/main (into-array [file-path]))
  (log/info "Processed a flux file:" file-path)
  (slurp out-path))

(defn process [data flux fix morph]
  (let [[out-path flux-content] (->flux-content data flux fix morph)]
    (-> flux-content
        (content->tempfile-path ".flux")
        (process-flux out-path))))
