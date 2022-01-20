(ns metafacture-playground.utils
  (:require
   [lambdaisland.uri :refer [uri query-string->map]]))

(defn parse-url [href]
  (let [query-params (-> href
                         uri
                         :query
                         query-string->map)]
    (if (:active-editor query-params)
      (update query-params :active-editor keyword)
      query-params)))
