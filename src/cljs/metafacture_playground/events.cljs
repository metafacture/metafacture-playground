(ns metafacture-playground.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [lambdaisland.uri :refer [uri join assoc-query* query-string->map query-encode]]
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
     (let [restored-key (->key key)
           parse-func (get-in db/db-parse-fns restored-key)
           restored-val (case val
                          "nil" nil
                          (parse-func val))]
       (assoc-in result restored-key restored-val)))
   {}
   web-storage))

;;; Size of editors

(defn update-width
  [{db :db} [_ editor content]]
  (let [visible-chars 50
        lines (clj-str/split-lines content)
        max-row-length (reduce (fn [current-max line]
                                 (if (> (count line) current-max)
                                   (count line)
                                   current-max))
                               0
                               lines)
        full-width (when (> max-row-length visible-chars) 16)
        editor (if (or (= editor :fix) (= editor :morph))
                 :switch
                 editor)]
    (if full-width
      {:db (assoc-in db [:input-fields editor :width] full-width)
       :storage/set {:session? true
                     :name (->storage-key [:input-fields editor :width])
                     :value full-width}}
      {:db (update-in db [:input-fields editor] dissoc :width)
       :storage/remove {:session? true
                        :name (->storage-key [:input-fields editor :width])}})))

(re-frame/reg-event-fx
 ::update-width
 update-width)

(defn window-resize
  [{db :db} [_ height]]
  {:db (assoc-in db [:ui :height] height)})

(re-frame/reg-event-fx
 ::window-resize
 window-resize)

;;; Message

  (defn dismiss-message
    [{db :db} _]
    {:db (assoc db :message nil)})

  (re-frame/reg-event-fx
   ::dismiss-message
   dismiss-message)

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
                   :name (->storage-key db-path) :value new-value}
     :dispatch [::update-width field-name new-value]}))

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
                 :pairs (generate-pairs {:input-fields sample})}
   :dispatch-n (mapv
                (fn [editor]
                  [::update-width editor (get-in sample [editor :content])])
                [:data :flux :fix])})

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
                       [:input-fields :fix :content]
                       [:input-fields :morph :content]
                       [:input-fields :data :width]
                       [:input-fields :flux :width]
                       [:input-fields :switch :width]]
        other-paths [[:result :content]
                     [:links :api-call]
                     [:links :workflow]]]
       {:db (clear-db db (concat storage-paths other-paths))
        :storage/remove {:session? true
                         :names (mapv ->storage-key storage-paths)}}))

(re-frame/reg-event-fx
 ::clear-all
 clear-all)

(defn switch-editor
  [{db :db} [_ editor]]
  {:db (assoc-in db [:input-fields :switch :active] editor)
   :dispatch [::update-width editor (get-in db [:input-fields editor :content])]
   :storage/set {:session? true
                 :name (->storage-key [:input-fields :switch :active])
                 :value (name editor)}})

(re-frame/reg-event-fx
 ::switch-editor
 switch-editor)

;;; Copy to clipboard

(defn copy-link
  [{:keys [db]} [_ val]]
  {:db db
   ::effects/copy-to-clipboard val})

(re-frame/reg-event-fx
 ::copy-link
 copy-link)

;;; Share links

(defn- get-used-params [flux fix morph]
  (if flux
    (let [flux (-> flux
                   (clj-str/replace "\n|" "|")
                   (clj-str/replace "\\s?\\|\\s?" "|"))
          fix-in-flux? (re-find #"\|fix\|" flux)
          morph-in-flux? (re-find #"\|morph\|" flux)]
      (merge
       (when fix-in-flux? {:fix fix})
       (when morph-in-flux? {:morph morph})))
    {}))

(defn- count-query-string [params]
  (reduce
   (fn [result [k v]]
     (if v
       (+ result
          (-> k name count)
          2                ;; '&' before key + '=' after key
          (-> v query-encode count))
       result))
   0
   params))

(defn generate-link [url path query-params]
  (-> (uri url)
      (join path)
      (assoc-query* query-params)
      str))

(defn generate-links
  [{db :db} [_ url data flux fix morph active-editor]]
  (let [max-query-string 1024
        max-url-string 2048
        api-call-params (merge (when data {:data data})
                               (when flux {:flux flux})
                               (get-used-params flux fix morph))
        workflow-params (merge (when data {:data data})
                               (when flux {:flux flux})
                               (when fix {:fix fix})
                               (when morph {:morph morph})
                               {:active-editor (name active-editor)})
        api-call-query-string-too-long? (> (count-query-string api-call-params)
                                           max-query-string)
        workflow-query-string-too-long? (> (count-query-string workflow-params)
                                           max-query-string)
        api-call-link (when (and api-call-params (not api-call-query-string-too-long?))
                        (generate-link url "./process" api-call-params))
        workflow-link (when (and workflow-params (not workflow-query-string-too-long?))
                        (generate-link url "" workflow-params))
        url-string-too-long? (or (> (count api-call-link) max-url-string)
                                 (> (count workflow-link) max-url-string))
        message (when (or api-call-query-string-too-long? workflow-query-string-too-long? url-string-too-long?)
                  {:content "Share links for large workflows are not supported yet"
                   :type :warning})]
    {:db (-> db
             (assoc :message message)
             (assoc-in [:links :api-call] (when-not url-string-too-long? api-call-link))
             (assoc-in [:links :workflow] (when-not url-string-too-long? workflow-link)))}))

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
  [{db :db} [_ {:keys [status status-text]}]]
  {:db (-> db
           (assoc-in [:result :loading?] false)
           (assoc :message {:content (str "Response from Server with "
                                          "Status-Code \"" status "\" and "
                                          "Status-Text \"" status-text \")
                            :type :error}))})

(re-frame/reg-event-fx
 ::bad-response
 bad-response)

(defn process
  [{db :db} [_ data flux fix morph active-editor]]
  (let [active-editor-in-flux? (re-find (re-pattern (str "\\|(\\s|\\n)*" (name active-editor) "(\\s|\\n)*\\|")) (or flux ""))
        message (when-not active-editor-in-flux?
                  (str "Flux does not use selected " (name active-editor) "."))]
    {:http-xhrio {:method          :get
                  :uri             "process"
                  :params {:data data
                           :flux flux
                           :fix fix
                           :morph morph}
                  :format (ajax/json-request-format)
                  :response-format (ajax/text-response-format)
                  :on-success      [::process-response]
                  :on-failure      [::bad-response]}
     :db (cond-> db
           true (assoc-in [:result :loading?] true)
           message (assoc :message {:content message
                                    :type :warning}))}))

(re-frame/reg-event-fx
 ::process
 process)

;;; Initialize-db

(defn deep-merge [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn- assoc-query-params [start-db {:keys [data flux fix morph active-editor]}]
  (cond-> start-db
    data (assoc-in [:input-fields :data :content] data)
    flux (assoc-in [:input-fields :flux :content] flux)
    fix (assoc-in [:input-fields :fix :content] fix)
    morph (assoc-in [:input-fields :morph :content] morph)
    active-editor (assoc-in [:input-fields :switch :active] (keyword active-editor))))

(defn initialize-db
  [{[_ href window-height] :event
    web-storage :storage/all}]
  (let [query-params (-> href uri :query query-string->map)]
    (if (empty? query-params)
      {:db (deep-merge db/default-db (restore-db web-storage) {:ui {:height window-height}})}
      {:db (-> db/default-db
               (assoc-query-params query-params)
               (assoc-in [:ui :height] window-height))
       :dispatch-n (mapv
                    (fn [editor]
                      [::update-width editor (get query-params editor)])
                    [:data :flux (-> query-params :active-editor keyword)])
       :storage/set {:session? true
                     :pairs (-> (assoc-query-params {} query-params)
                                generate-pairs)}
       ::effects/unset-url-query-params href})))

(re-frame/reg-event-fx
 ::initialize-db
[(re-frame/inject-cofx :storage/all {:session? true})]
 initialize-db)
