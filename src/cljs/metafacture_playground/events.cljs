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
  (assoc db
         :fields
         db/sample-fields))

(re-frame/reg-event-db
  :load-sample
  load-sample)

(defn clear-all
  [db _]
  (assoc db :fields {:data ""
                     :flux ""
                     :fix  ""}))

(re-frame/reg-event-db
 :clear-all
 clear-all)

(defn initialize-db
  [_ _]
  db/default-db)

(re-frame/reg-event-db
 ::initialize-db
 initialize-db)
