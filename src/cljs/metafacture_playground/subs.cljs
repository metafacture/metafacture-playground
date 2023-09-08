(ns metafacture-playground.subs
  (:require
   [re-frame.core :as re-frame]
   [clojure.string :as clj-str]))

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

(re-frame/reg-sub
 ::examples
 (fn [db _]
   (get db :examples)))

(re-frame/reg-sub
 ::label
 (fn [db [_ editor]]
   (get-in db [:editors editor :label])))

(re-frame/reg-sub
 ::file-variable
 (fn [db [_ editor]]
   (get-in db [:editors editor :file-variable])))

(re-frame/reg-sub
 ::key-count
 (fn [db [_ editor-name]]
   (get-in db [:editors editor-name :key-count])))

(re-frame/reg-sub
 ::editor-content
 (fn [db [_ editor]]
   (get-in db [:editors editor :content])))

(defn- editor-height-maximum [height font-size height-divider]
  (-> height
      (/ (or height-divider 1))
      (/ font-size) ; convert px in em
      (- 10)))

(re-frame/reg-sub
 ::height
 (fn [db [_ editor min-editor-size font-size]]
   (let [height-divider (get-in db [:editors editor :height-divider])
         max-editor-size (-> (get-in db [:ui :height])
                             (editor-height-maximum font-size height-divider))
         calculated-size (-> (get-in db [:editors editor :content])
                             (clj-str/split #"\r?\n" -1)
                             count
                             (+ 3))]
     (-> (min calculated-size max-editor-size)
         (max min-editor-size)
         (str "em")))))

(re-frame/reg-sub
 ::width
 (fn [db [_ editor]]
   (get-in db [:editors editor :width])))

(re-frame/reg-sub
 ::monaco-language
 (fn [db [_ editor]]
   (get-in db [:editors editor :language])))

(re-frame/reg-sub
 ::collapsed?
 (fn [db [_ editor]]
   (get-in db [:editors editor :collapsed?])))

(re-frame/reg-sub
 ::disabled?
 (fn [db [_ editor]]
   (get-in db [:editors editor :disabled?])))

(re-frame/reg-sub
 ::link
 (fn [db [_ type]]
   (get-in db [:links type])))

(re-frame/reg-sub
 ::result-loading?
 (fn [db _]
   (get-in db [:editors :result :loading?])))

(re-frame/reg-sub
 ::backend-versions
 (fn [db _]
   (get db :versions)))
