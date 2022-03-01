(ns metafacture-playground.subs
  (:require
   [re-frame.core :as re-frame]
   [clojure.string :as clj-str]
   [metafacture-playground.utils :as utils]))

(defn- expand-indentation [details]
  (when details
    (-> details
        (clj-str/replace #"\n\s{4}\b" "\n        ")
        (clj-str/replace #"\n\s\b" "\n    "))))

(re-frame/reg-sub
  ::message
  (fn [db _]
    (-> (get db :message)
        (update :details expand-indentation))))

(re-frame/reg-sub
 ::error-details-visible?
 (fn [db _]
   (get-in db [:message :show-details?])))

(re-frame/reg-sub
 ::dropdown-active-item
 (fn [db _]
   (get-in db [:ui :dropdown :active-item])))

(re-frame/reg-sub
 ::dropdown-open?
 (fn [db [_ folder]]
   (get-in db [:ui :dropdown folder :open?])))

(defn- display-name [str]
  (clj-str/replace str "_" " "))

(defn- examples->dropdown-entries []
  (map
   (fn [[k v]]
     (if (map? v)
       {k (into (sorted-map)
                (examples->dropdown-entries)
                v)}
       {k {:display-name (display-name k)
           :value (utils/parse-url v)}}))))

(re-frame/reg-sub
 ::examples
 (fn [db _]
   (into (sorted-map)
         (examples->dropdown-entries)
         (get db :examples))))

(re-frame/reg-sub
 ::field-value
 (fn [db [_ field-name]]
   (get-in db [:input-fields field-name :content])))

(defn- editor-height-maximum [height font-size height-divider]
  (-> height
      (/ (or height-divider 1))
      (/ font-size) ; convert px in em
      (- 10)))

(re-frame/reg-sub
 ::editor-height
 (fn [db [_ editor-name min-editor-size font-size height-divider]]
   (let [max-editor-size (-> (get-in db [:ui :height])
                             (editor-height-maximum font-size height-divider))
         calculated-size (-> (get-in db [:input-fields editor-name :content])
                             (clj-str/split #"\r?\n" -1)
                             count
                             (+ 3))]
     (-> (min calculated-size max-editor-size)
         (max min-editor-size)
         (str "em")))))

(re-frame/reg-sub
 ::editor-width
 (fn [db [_ editor]]
   (let [width (get-in db [:input-fields editor :width])]
     (case editor
       :flux (if (= 16 (get-in db [:input-fields :switch :width]))
               16
               width)
       :switch (if (= 16 (get-in db [:input-fields :flux :width]))
              16
              width)
       width))))

(re-frame/reg-sub
 ::collapsed?
 (fn [db [_ path]]
   (get-in db (conj path :collapsed?))))

(re-frame/reg-sub
 ::disabled?
 (fn [db [_ editor]]
   (get-in db [:input-fields editor :disabled?])))

(re-frame/reg-sub
 ::active-editor
 (fn [db _]
   (get-in db [:input-fields :switch :active])))

(re-frame/reg-sub
 ::process-result
 (fn [db _]
   (get-in db [:result :content])))

(re-frame/reg-sub
 ::link
 (fn [db [_ type]]
   (get-in db [:links type])))

(re-frame/reg-sub
 ::result-loading?
 (fn [db _]
   (get-in db [:result :loading?])))

(re-frame/reg-sub
 ::backend-versions
 (fn [db _]
   (get db :versions)))
