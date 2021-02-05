(ns metafacture-playground.events
  (:require
   [re-frame.core :as re-frame]
   [metafacture-playground.db :as db]))

(defn edit-value
  [db [_ path new-value]]
  (assoc-in db path new-value))

(re-frame/reg-event-db
 :edit
 edit-value)

(defn load-sample
  [db _]
  (reduce
   (fn [db [k sample-v]]
     (assoc-in db [:fields k] sample-v))
   db
   db/sample-fields))

(re-frame/reg-event-db
  :load-sample
  load-sample)

(defn clear-all
  [db _]
  (let [fields [:data :flux :fix]]
    (reduce
     (fn [db field-to-empty]
       (assoc-in db [:fields field-to-empty] ""))
     db
     fields)))

(re-frame/reg-event-db
 :clear-all
 clear-all)

(defn initialize-db
  [_ _]
  db/default-db)

(re-frame/reg-event-db
 ::initialize-db
 initialize-db)
