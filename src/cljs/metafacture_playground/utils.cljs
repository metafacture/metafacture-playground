(ns metafacture-playground.utils
  (:require
   [lambdaisland.uri :refer [uri query-string->map]]
   [clojure.string :as clj-str]))

(defn display-name [s]
  (clj-str/replace s "_" " "))

(defn parse-url [href]
  (let [query-params (-> href
                         uri
                         :query
                         query-string->map)]
     query-params))
