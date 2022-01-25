(ns metafacture-playground.effects
  (:require [re-frame.core :as re-frame]
            [clojure.string :as clj-str]
            [cljs.pprint :as pp]))

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

(defn- file-blob [datamap mimetype]
  (js/Blob. [datamap] {"type" mimetype}))

(defn- link-for-blob [blob filename]
  (doto (.createElement js/document "a")
    (set! -download filename)
    (set! -href (.createObjectURL js/URL blob))))

(defn- click-and-remove-link [link]
  (let [click-remove-callback
        (fn []
          (.dispatchEvent link (js/MouseEvent. "click"))
          (.removeChild (.-body js/document) link))]
    (.requestAnimationFrame js/window click-remove-callback)))

(defn- add-link [link]
  (.appendChild (.-body js/document) link))

(defn- download-data [data filename mimetype]
  (-> data
      (file-blob mimetype)
      (link-for-blob filename)
      add-link
      click-and-remove-link))

(re-frame/reg-fx
 ::export-files
 (fn [exports]
   (doseq [[export-data file-name] exports]
       (download-data export-data file-name "text/plain"))))
