(ns metafacture-playground.db)

(def default-db
  {:editors {:data {:key-count 0
                    :content nil
                    :collapsed? false
                    :disabled? true
                    :label "\"inputFile\"-content"
                    :file-variable "inputFile"
                    :width 16
                    :language "text/plain"
                    :height-divider 3}
             :flux {:key-count 0
                    :content nil
                    :collapsed? false
                    :default-width 8
                    :width 8
                    :label "Flux File"
                    :language "text/plain"}
             :transformation {:key-count 0
                              :content nil
                              :collapsed? false
                              :default-width 8
                              :width 8
                              :disabled? true
                              :label "\"transformationFile\"-content"
                              :file-variable "transformationFile"
                              :language "text/plain"}
             :result {:label "Result"
                      :loading? false
                      :collapsed? false
                      :content nil
                      :width 16
                      :language "text/plain"}}
   :links {:api-call nil
           :workflow nil}
   :message {:content nil
             :details nil
             :show-details? false
             :type nil}
   :ui {:height nil
        :dropdown {:active-item nil
                   "main" {:open? false}}}})

(defn- parseBoolean [val]
  (if (= val "true")
    true
    false))

(def db-parse-fns
  {:editors {:data {:content str
                    :collapsed? parseBoolean
                    :width int
                    :disabled? parseBoolean}
             :flux {:content str
                    :collapsed? parseBoolean
                    :width int}
             :transformation {:content str
                              :disabled? parseBoolean
                              :collapsed? parseBoolean
                              :width int}
             :result {:content str
                      :disabled? parseBoolean
                      :collapsed? parseBoolean
                      :width int
                      :loading? parseBoolean}}
   :links {:api-call str
           :workflow str}
   :message {:content str
             :type keyword}
   :ui {:height parseBoolean
        :dropdown {:active-item #(if (= "" %) nil (str %))}}})
