(ns metafacture-playground.process
  (:require
   [clojure.string :as clj-str]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io])
  (:import
   (java.io File)
   (org.metafacture.runner Flux)))

(defn- content->tempfile-path [content file-extension]
  (let [tmp-directory (-> (System/getProperty "user.dir")
                          (str File/separator "tmp")
                          (File.))]
    (when-not (.exists tmp-directory)
      (log/info "Creating directory: " (.getAbsolutePath tmp-directory))
      (.mkdir tmp-directory))
    (let [temp-file (File/createTempFile "metafix" file-extension tmp-directory)]
      (spit temp-file content)
      (let [file-path (clj-str/replace (.getAbsolutePath temp-file) "\\" "/")]
        (log/info "Wrote content to temp file:" file-path)
        (log/trace "Content" content)
        file-path))))

(defn- delete-files! [file-pathes]
  (doseq [file-path file-pathes]
    (try
      (io/delete-file file-path)
      (log/info "Deleted file " file-path)
      (catch Exception e
        (log/warn "Could not delete file " file-path ". Catched exception: " (.getMessage e))))))


(defn process [flux data transformation]
  (let [inputfile (content->tempfile-path data ".data")
        transformationfile (content->tempfile-path transformation ".fix")
        out-path (content->tempfile-path "" ".txt")
        output (str "|write(\"" out-path "\");")
        flux (-> (str "default inputFile = \"" inputfile "\";\n"
                      "default transformationFile = \"" transformationfile "\";\n"
                      flux)
                 (clj-str/replace #"\|(\s*|\n*)write\(\".*\"\)(\s*|\n*);" output)
                 (clj-str/replace #"\|(\s*|\n*)print(\s*|\n*);" output))
        fluxfile (content->tempfile-path flux ".flux")]
    (Flux/main (into-array [fluxfile]))
    (log/info "Executed flux file with Flux/main. Result in" out-path)
    (let [result (slurp out-path)]
      (delete-files! [inputfile transformationfile fluxfile out-path])
      result)))
