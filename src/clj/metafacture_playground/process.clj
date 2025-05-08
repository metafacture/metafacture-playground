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


(defn process [flux data transformation]
  (let [inputfile (content->tempfile-path data ".data")
        transformationFile (content->tempfile-path transformation ".fix")
        out-path (content->tempfile-path "" ".txt")
        output (str "|write(\"" out-path "\");")
        flux-str (-> (str "default inputFile = \"" inputfile "\";\n"
                          "default transformationFile = \"" transformationFile "\";\n"
                          flux)
                     ;; Ersetze vorhandene |write(...) oder |print durch unsere Ausgabeanweisung
                     (clj-str/replace #"\|(\s*|\n*)write\(\".*\"\)(\s*|\n*);" output)
                     (clj-str/replace #"\|(\s*|\n*)print(\s*|\n*);" output))
        fluxfile (content->tempfile-path flux-str ".flux")
        max-size-bytes (* 1024 1024 1024)] ;; 1 GB

    (try
      ;; Führe das Flux-Programm aus
      (Flux/main (into-array [fluxfile]))
      (let [outfile (io/file out-path)]
        (if (> (.length outfile) max-size-bytes)
          (throw (ex-info "Output file exceeds maximum allowed size (1 GB)" {:file out-path}))
          (do
            (log/info "Executed flux file with Flux/main. Result in" out-path)
            (slurp outfile))))
      (finally
        ;; Aufräumen der temporären Dateien
        (doseq [f [inputfile transformationFile fluxfile out-path]]
          (try
            (let [file (io/file f)]
              (when (.exists file)
                (io/delete-file file true)))
            (catch Exception e
              (log/warn "Could not delete temp file:" f e))))))))
