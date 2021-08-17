(ns metafacture-playground.subs
  (:require
   [re-frame.core :as re-frame]
   [clojure.string :as clj-str]))

(re-frame/reg-sub
  ::message
  (fn [db _]
    (get db :message)))

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
       :flux (if (= 16 (get-in db [:input-fields :fix :width]))
               16
               width)
       :fix (if (= 16 (get-in db [:input-fields :flux :width]))
              16
              width)
       width))))

(re-frame/reg-sub
 ::collapsed?
 (fn [db [_ path]]
   (get-in db (conj path :collapsed?))))

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
