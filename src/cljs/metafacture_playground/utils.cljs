(ns metafacture-playground.utils
  (:require
   [lambdaisland.uri :refer [uri query-string->map]]))

(defn parse-url [href]
  (-> href uri :query query-string->map))
