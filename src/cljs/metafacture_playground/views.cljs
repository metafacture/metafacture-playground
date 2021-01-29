(ns metafacture-playground.views
  (:require
   [re-frame.core :as re-frame]
   [metafacture-playground.subs :as subs]
   [clojure.string :as clj-str]))

(defn control-panel []
  [:div.row
   [:input {:type "button"
            :value "Load sample"
            :on-click #(re-frame/dispatch [:load-sample])}]
   [:input {:type "button"
            :value "Clear all"
            :on-click #(re-frame/dispatch [:clear-all])}]])

(defn field-row [field-name]
  (let [field-value(re-frame/subscribe [::subs/field-value [:fields field-name]])]
    (fn [field-name]
      [:div.row (str (-> field-name name clj-str/capitalize) ": ")
       [:input {:type "text"
                :id (name field-name)
                :value @field-value
                :on-change #(re-frame/dispatch [:edit [:fields field-name] (-> % .-target .-value)])}]])))

(defn editor []
  (let [fix  (re-frame/subscribe [::subs/field-value [:fields :fix]])]
    (fn []
      [:div.col "Fix:"
       [:textarea {:id "fix"
                   :value @fix
                   :on-change #(re-frame/dispatch [:edit [:fields :fix] (-> % .-target .-value)])}]])))

(defn result-panel []
  [:div.col
   [:input {:id "result"
            :value "This could be your result"}]])

(defn main-panel []
  [:div.container
   [:h1 "Metafacture Playground"]
   [control-panel]
   [field-row :data]
   [field-row :flux]
   [:div.row
    [editor]
    [result-panel]]])
