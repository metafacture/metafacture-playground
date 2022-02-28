(ns metafacture-playground.db)

(def default-db
  {:input-fields {:data {:content nil
                         :collapsed? false
                         :width nil
                         :disabled? true}
                  :flux {:content nil
                         :collapsed? false
                         :width nil}
                  :fix {:content nil
                        :disabled? true}
                  :morph {:content nil
                          :disabled? true}
                  :switch {:collapsed? false
                           :active :fix
                           :width nil}}
   :links {:api-call nil
           :workflow nil}
   :result {:loading? false
            :collapsed? false
            :content nil}
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
  {:input-fields {:data {:content str
                         :collapsed? parseBoolean
                         :width int
                         :disabled? parseBoolean}
                  :flux {:content str
                         :collapsed? parseBoolean
                         :width int}
                  :fix {:content str
                        :disabled? parseBoolean}
                  :morph {:content str
                          :disabled? parseBoolean}
                  :switch {:collapsed? parseBoolean
                           :active keyword
                           :width int}}
   :links {:api-call str
           :workflow str}
   :result {:loading? parseBoolean
            :collapsed? parseBoolean
            :content str}
   :message {:content str
             :type keyword}
   :ui {:height parseBoolean
        :dropdown {:active-item str}}})
