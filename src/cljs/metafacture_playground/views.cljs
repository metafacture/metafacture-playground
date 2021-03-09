(ns metafacture-playground.views
  (:require
   [re-frame.core :as re-frame]
   [metafacture-playground.subs :as subs]
   [clojure.string :as clj-str]))

(defn header []
  [:div.row
   [:div.col
    [:h1.header  "Metafacture Playground"]]])

(defn process-button []
  (let [data (re-frame/subscribe [::subs/field-value :data])
        flux (re-frame/subscribe [::subs/field-value :flux])
        fix  (re-frame/subscribe [::subs/field-value :fix])]
    (fn []
      [:input.simple-button {:type "button"
                             :value "Process"
                             :on-click #(re-frame/dispatch [:process @data @flux @fix])}])))

(defn control-panel []
  [:div.row
   [:div.col
    [:input.simple-button {:type "button"
                           :value "Load sample"
                           :on-click #(re-frame/dispatch [:load-sample])}]
    [:input.simple-button {:type "button"
                           :value "Clear all"
                           :on-click #(re-frame/dispatch [:clear-all])}]
    [process-button]]])

(defn field-row [field-name]
  (let [field-value (re-frame/subscribe [::subs/field-value field-name])]
    (fn [field-name]
      [:div.row
       [:div.col-1
        [:p (str (-> field-name name clj-str/capitalize) ": ")]]
       [:div.col
        [:input.simple-input {:type "text"
                              :id (name field-name)
                              :value @field-value
                              :on-change #(re-frame/dispatch [:edit-input-value field-name (-> % .-target .-value)])}]]])))

(defn editor []
  (let [fix  (re-frame/subscribe [::subs/field-value :fix])]
    (fn []
      [:div.col
       [:textarea {:id "editor"
                   :value @fix
                   :rows 25
                   :cols 40
                   :on-change #(re-frame/dispatch [:edit-input-value :fix (-> % .-target .-value)])}]])))

(defn result-panel []
  (let [result (re-frame/subscribe [::subs/process-result])
        loading? (re-frame/subscribe [::subs/result-loading?])]
    (fn []
      [:div.col
       (if @loading?
         [:div.loader]
         [:textarea {:id "result"
                     :value @result
                     :rows 25
                     :cols 40
                     :readOnly true}])])))

(defn main-panel []
  [:div.container
   [header]
   [control-panel]
   [field-row :data]
   [field-row :flux]
   [:div.row
    [editor]
    [result-panel]]])
