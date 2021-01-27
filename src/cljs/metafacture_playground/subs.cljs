(ns metafacture-playground.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::field-value
  (fn [db [_ path-to-field]]
    (get-in db path-to-field)))
