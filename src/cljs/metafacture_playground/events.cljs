(ns metafacture-playground.events
  (:require
   [re-frame.core :as re-frame]
   [metafacture-playground.db :as db]))

(re-frame/reg-event-db
   :edit
   (fn [db [_ path new-value]]
     (assoc-in db path new-value)))

(re-frame/reg-event-db
  :load-sample
  (fn [db _]
    (assoc db :fields {:data "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}\n 2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}"
                       :flux "as-lines|decode-formeta|fix|stream-to-xml(rootTag=\"collection\")"
                       :fix  "map(_id, id)\nmap(a,title)\nmap(b.n,author)\n/*map(_else)*/\n"})))

(re-frame/reg-event-db
 :clear-all
 (fn [db _]
   (assoc db :fields {:data ""
                      :flux ""
                      :fix ""})))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
