(ns metafacture-playground.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::field-value
  (fn [db [_ field-name]]
    (get-in db [:input-fields field-name])))

(re-frame/reg-sub
 ::process-result
 (fn [db _]
   (get-in db [:result :content])))

(re-frame/reg-sub
 ::result-loading?
 (fn [db _]
   (get-in db [:result :loading?])))
