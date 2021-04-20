(ns metafacture-playground.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [goog.uri.utils :as goog-uri]
   [metafacture-playground.db :as db]
   [metafacture-playground.effects]))

;;; Collapsing panels

(defn collapse-panel
  [db [_ path status]]
  (assoc-in db
            (conj path :collapsed?)
            (not status)))

  (re-frame/reg-event-db
   :collapse-panel
   collapse-panel)

;;; Editing input fields

(defn edit-value
  [db [_ field-name new-value]]
  (assoc-in db [:input-fields field-name :content] new-value))

(re-frame/reg-event-db
 :edit-input-value
 edit-value)

(defn load-sample
  [db _]
  (reduce
   (fn [db [k sample-v]]
     (assoc-in db [:input-fields k] sample-v))
   db
   db/sample-fields))

(re-frame/reg-event-db
  :load-sample
  load-sample)

(defn clear-all
  [db _]
  (let [paths [[:input-fields :data :content]
               [:input-fields :flux :content]
               [:input-fields :fix :content]
               [:result :content]
               [:result :links :api-call]]]
    (reduce
     (fn [db path]
       (assoc-in db path nil))
     db
     paths)))

(re-frame/reg-event-db
 :clear-all
 clear-all)

;;; Copy to clipboard

(defn copy-link
  [{:keys [db]} [_ val]]
  {:db db
   :copy-to-clipboard val})

(re-frame/reg-event-fx
 :copy-link
 copy-link)

;;; Share links

(defn generate-links
  [db [_ data flux fix]]
  (let [url (-> js/window .-location .-href)]
    (assoc-in db
              [:result :links :api-call]
              (goog-uri/appendParamsFromMap
               (str url "process")
               #js {:data data
                    :flux flux
                    :fix fix}))))

(re-frame/reg-event-db
 :generate-links
 generate-links)

;;; Processing

(defn process-response
  [db [_ response]]
  (-> db
      (assoc-in [:result :loading?] false)
      (assoc-in [:result :content] response)))

(re-frame/reg-event-db                   
 :process-response             
  process-response)

(defn bad-response
  [db [_ response]]
  (-> db
      (assoc-in [:result :loading?] false)
      (assoc-in [:result :content] "Bad response")))

(re-frame/reg-event-db
 :bad-response
 bad-response)

(defn process
  [{:keys [db]} [_ data flux fix]]
  {:db  (assoc-in db [:result :loading?] true)
   :fx [[:http-xhrio {:method          :get
                      :uri             "process"
                      :params {:data data
                               :flux flux
                               :fix fix}
                      :format (ajax/json-request-format)
                      :response-format (ajax/text-response-format)
                      :on-success      [:process-response]
                      :on-failure      [:bad-response]}]
        [:dispatch [:generate-links data flux fix]]]})

(re-frame/reg-event-fx
 :process
 process)

;;; Initialize-db

(defn initialize-db
  [_ _]
  db/default-db)

(re-frame/reg-event-db
 ::initialize-db
 initialize-db)
