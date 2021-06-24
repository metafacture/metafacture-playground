(ns metafacture-playground.effects
  (:require [re-frame.core :as re-frame]
            [clojure.string :as clj-str]))

(re-frame/reg-fx
 ::copy-to-clipboard
 (fn [val]
   (let [el (js/document.createElement "textarea")]
     (set! (.-value el) val)
     (.appendChild js/document.body el)
     (.select el)
     (js/document.execCommand "copy")
     (.removeChild js/document.body el))))

(re-frame/reg-fx
 ::unset-url-query-params
 (fn [href]
   (let [new-href (clj-str/replace href #"\?.*" "")]
     (-> js/window .-history (.replaceState {} "" new-href)))))
