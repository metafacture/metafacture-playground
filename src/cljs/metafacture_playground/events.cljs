(ns metafacture-playground.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [lambdaisland.uri :refer [uri join assoc-query* query-string->map]]
   [metafacture-playground.db :as db]
   [metafacture-playground.effects :as effects]
   [com.degel.re-frame.storage]
   [clojure.string :as clj-str]))

;;; Utils for web storage use

(defn- ->storage-key [key]
  (if (seq key)
    (clj-str/join ">" key)
    key))

(defn- ->key [storage-key]
  (if (clj-str/includes? storage-key ">")
    (->> (clj-str/split storage-key #">")
         (mapv #(clj-str/replace-first % #"\:" ""))
         (mapv keyword))
    storage-key))

  (defn- generate-pairs [db]
    (reduce
     (fn [pairs [k rest-db]]
       (concat pairs
               (mapcat
                (fn [sub-key]
                  (mapv
                   (fn [sub-sub-key]
                     {:name (->storage-key [k sub-key sub-sub-key])
                      :value (get-in db [k sub-key sub-sub-key])})
                   (-> (get rest-db sub-key) keys)))
                (keys rest-db))))
     []
     db))

  (defn- restore-db [web-storage]
    (reduce
     (fn [result [key val]]
       (let [restored-val (case val
                            "true" true
                            "false" false
                            val)]
         (assoc-in result (->key key) restored-val)))
     {}
     web-storage))

;;; Collapsing panels

  (defn collapse-panel
    [{:keys [db]} [_ path status]]
    (let [db-path (conj path :collapsed?)
          new-value (not status)]
      {:db (assoc-in db db-path new-value)
       :storage/set {:session? true
                     :name (->storage-key db-path) :value new-value}}))

  (re-frame/reg-event-fx
   ::collapse-panel
   collapse-panel)

;;; Editing input fields

(defn edit-value
  [{db :db} [_ field-name new-value]]
  (let [db-path [:input-fields field-name :content]]
    {:db (assoc-in db db-path new-value)
     :storage/set {:session? true
                   :name (->storage-key db-path) :value new-value}}))

(re-frame/reg-event-fx
 ::edit-input-value
 edit-value)

(defn- add-sample [db sample]
  (reduce
   (fn [db [k sample-v]]
     (assoc-in db [:input-fields k] sample-v))
   db
   sample))

(defn load-sample
  [{db :db} [_ sample]]
  {:db (add-sample db sample)
   :storage/set {:session? true
                 :pairs (generate-pairs {:input-fields sample})}})

(re-frame/reg-event-fx
  ::load-sample
  load-sample)

(defn- clear-db [db paths]
  (reduce
   (fn [db path]
     (assoc-in db path nil))
   db
   paths))

(defn clear-all
  [{db :db} _]
  (let [storage-paths [[:input-fields :data :content]
                       [:input-fields :flux :content]
                       [:input-fields :fix :content]]
        other-paths [[:result :content]
                     [:links :api-call]
                     [:links :workflow]]]
       {:db (clear-db db (concat storage-paths other-paths))
        :storage/remove {:session? true
                         :names (mapv ->storage-key storage-paths)}}))

(re-frame/reg-event-fx
 ::clear-all
 clear-all)

;;; Copy to clipboard

(defn copy-link
  [{:keys [db]} [_ val]]
  {:db db
   ::effects/copy-to-clipboard val})

(re-frame/reg-event-fx
 ::copy-link
 copy-link)

;;; Share links

(defn generate-link [url path query-params]
  (-> (uri url)
      (join path)
      (assoc-query* query-params)
      str))

(defn generate-links
  [{db :db} [_ url data flux fix]]
  (if-let [query-params (merge (when data {:data data})
                               (when flux {:flux flux})
                               (when fix {:fix fix}))]
    {:db (-> db
         (assoc-in [:links :api-call] (generate-link url "./process" query-params))
         (assoc-in [:links :workflow] (generate-link url "" query-params)))}
    {:db (-> db
             (assoc-in [:links :api-call] nil)
             (assoc-in [:links :workflow] nil))}))

(re-frame/reg-event-fx
 ::generate-links
 generate-links)

;;; Processing

(defn process-response
  [{db :db} [_ response]]
  {:db (-> db
           (assoc-in [:result :loading?] false)
           (assoc-in [:result :content] response))})

  (re-frame/reg-event-fx
   ::process-response
   process-response)

(defn bad-response
  [{db :db} [_ response]]
  {:db (-> db
           (assoc-in [:result :loading?] false)
           (assoc-in [:result :content] "Bad response"))})

(re-frame/reg-event-fx
 ::bad-response
 bad-response)

(defn process
  [{db :db} [_ data flux fix]]
  {:http-xhrio {:method          :get
                :uri             "process"
                :params {:data data
                         :flux flux
                         :fix fix}
                :format (ajax/json-request-format)
                :response-format (ajax/text-response-format)
                :on-success      [::process-response]
                :on-failure      [::bad-response]}
   :db (assoc-in db [:result :loading?] true)})

(re-frame/reg-event-fx
 ::process
 process)

;;; Initialize-db

(defn deep-merge [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn- assoc-query-params [start-db {:keys [data flux fix]}]
  (cond-> start-db
    data (assoc-in [:input-fields :data :content] data)
    flux (assoc-in [:input-fields :flux :content] flux)
    fix (assoc-in [:input-fields :fix :content] fix)))

(defn initialize-db
  [{[_ href] :event
    web-storage :storage/all}]
  (let [query-params (-> href uri :query query-string->map)]
    (if (empty? query-params)
      {:db (deep-merge db/default-db (restore-db web-storage))}
      {:db (assoc-query-params db/default-db query-params)
       :storage/set {:session? true
                     :pairs (-> (assoc-query-params {} query-params)
                                generate-pairs)}
       ::effects/unset-url-query-params href})))

(re-frame/reg-event-fx
 ::initialize-db
[(re-frame/inject-cofx :storage/all {:session? true})]
 initialize-db)
