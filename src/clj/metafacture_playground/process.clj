(ns metafacture-playground.process
  (:require
   [clojure.string :as clj-str]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log])
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

(defn- remove-temp-files [temp-files]
  (doseq [temp-file temp-files]
          (try
            (let [file (io/file temp-file)]
              (when (.exists file)
                (io/delete-file file false)))
            (catch Exception e
              (log/warn "Could not delete temp file:" temp-file e)))))

(defn process [flux data transformation]
  (let [input-file (content->tempfile-path data ".data")
        transformation-file (content->tempfile-path transformation ".fix")
        out-path (content->tempfile-path "" ".txt")
        output (str "|write(\"" out-path "\");")
        flux (-> (str "default inputFile = \"" input-file "\";\n"
                      "default transformationFile = \"" transformation-file "\";\n"
                      flux)
                 (clj-str/replace #"\|(\s*|\n*)write\(\".*\"\)(\s*|\n*);" output)
                 (clj-str/replace #"\|(\s*|\n*)print(\s*|\n*);" output))
        flux-file (content->tempfile-path flux ".flux")]
    (try
      (Flux/main (into-array [flux-file]))
      (log/info "Executed flux file with Flux/main. Result in" out-path)
      (slurp out-path)
      (finally
        (remove-temp-files [input-file transformation-file flux-file out-path])))))
