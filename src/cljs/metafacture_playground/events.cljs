(ns metafacture-playground.events
  (:require
   [metafacture-playground.db :as db]
   [metafacture-playground.effects :as effects]
   [metafacture-playground.utils :as utils]
   [re-frame.core :as re-frame]
   [day8.re-frame.fetch-fx]
   [jtk-dvlp.re-frame.readfile-fx]
   [lambdaisland.uri :refer [uri join assoc-query* query-encode]]
   [com.degel.re-frame.storage]
   [clojure.string :as clj-str]
   [clojure.walk :as walk]
   [cognitect.transit :as transit]
   [goog.object :as g]))

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
                    :value (let [value (get-in db [k sub-key sub-sub-key])]
                             (if (keyword? value)
                               (name value)
                               value))})
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

(defn show-error-details
  [{db :db} [_ val]]
  {:db (assoc-in db [:message :show-details?] val)})

(re-frame/reg-event-fx
 ::show-error-details
 show-error-details)

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
  [{db :db} [_ field-name new-value & is-example-data?]]
  (let [db-path [:input-fields field-name :content]
        disable-editors (when (= field-name :flux)
                          (let [val (-> new-value
                                        (clj-str/replace #"\n*\|" "|")
                                        (clj-str/replace #"\s*\|\s*" "|"))
                                data-used? (boolean (re-find #"PG_DATA" val))
                                morph-used? (boolean (re-find #"\|morph\|" val))
                                fix-used? (boolean (re-find #"\|fix\|" val))]
                            {[:input-fields :data :disabled?] (not data-used?)
                             [:input-fields :morph :disabled?] (not morph-used?)
                             [:input-fields :fix :disabled?] (not fix-used?)}))]
    {:db (cond-> (reduce
                  (fn [db [path v]]
                    (assoc-in db path v))
                  db
                  disable-editors)
           true (assoc-in db-path new-value)
           (not is-example-data?) (assoc-in [:ui :dropdown :active-item] nil))
     :storage/set {:session? true
                   :pairs (conj
                           (mapv
                            (fn [[db-path v]]
                              {:name (->storage-key db-path) :value v})
                            disable-editors)
                           {:name (->storage-key db-path) :value new-value}
                           (when-not is-example-data? {:name (->storage-key [:ui :dropdown :active-item]) :value nil}))}
     :dispatch [::update-width field-name new-value]}))

(re-frame/reg-event-fx
 ::edit-input-value
 edit-value)

(defn open-dropdown
  [{db :db} [_ folder status]]
  {:db (assoc-in db [:ui :dropdown folder :open?] status)})

(re-frame/reg-event-fx
 ::open-dropdown
 open-dropdown)

(defn load-sample
  [{db :db} [_ dropdown-value sample]]
    {:db (-> db
             (assoc :result nil)
             (assoc-in [:ui :dropdown :active-item] dropdown-value))
     :fx (conj
          (mapv
           (fn [editor]
             [:dispatch [::edit-input-value editor (get sample editor "") true]])
           [:data :flux :fix :morph])
          [:dispatch [::switch-editor (:active-editor sample)]])
     :storage/set {:session? true
                   :name (->storage-key [:ui :dropdown :active-item])
                   :value dropdown-value}})

(re-frame/reg-event-fx
  ::load-sample
  load-sample)

(defn- clear-db [db paths]
  (reduce
   (fn [db [path v]]
     (assoc-in db path v))
   db
   paths))

(defn clear-all
  [{db :db} _]
  (let [storage-remove [[[:input-fields :data :content] nil]
                        [[:input-fields :flux :content] nil]
                        [[:input-fields :fix :content] nil]
                        [[:input-fields :morph :content] nil]
                        [[:input-fields :data :width] nil]
                        [[:input-fields :flux :width] nil]
                        [[:input-fields :switch :width] nil]]
        storage-set [[[:input-fields :data :disabled?] true]
                     [[:input-fields :fix :disabled?] true]
                     [[:input-fields :morph :disabled?] true]
                     [[:ui :dropdown :active-item] nil]]
        other [[[:result :content] nil]
               [[:links :api-call] nil]
               [[:links :workflow] nil]]]
       {:db (clear-db db (concat storage-remove other storage-set))
        :storage/remove {:session? true
                         :names (mapv #(->storage-key (first %)) storage-remove)}
        :storage/set {:session? true
                      :pairs (mapv (fn [[path v]]
                                     {:name (->storage-key path)
                                      :value v})
                                   storage-set)}}))

(re-frame/reg-event-fx
 ::clear-all
 clear-all)

(defn switch-editor
  [{db :db} [_ editor]]
  (let [editor (or editor :fix)]
    (merge
     {:db (assoc-in db [:input-fields :switch :active] editor)
      :storage/set {:session? true
                    :name (->storage-key [:input-fields :switch :active])
                    :value (when editor (name editor))}}
     (when editor
       {:dispatch [::update-width editor (get-in db [:input-fields editor :content])]}))))

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

(defn- get-used-params [flux fix morph data]
  (if flux
    (let [flux (-> flux
                   (clj-str/replace #"\n\|" "|")
                   (clj-str/replace #"\s*\|\s*" "|"))
          fix-in-flux? (re-find #"\|fix\|" flux)
          morph-in-flux? (re-find #"\|morph\|" flux)
          data-in-flux? (re-find #"PG_DATA" flux)]
      (cond-> {}
        fix-in-flux? (merge {:fix fix})
        morph-in-flux? (merge {:morph morph})
        data-in-flux? (merge {:data data})))
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

(defn- query-string-too-long? [params maximum]
  (try
    (> (count-query-string params)
       maximum)
    (catch js/RangeError _
      true)))

(defn generate-link [url path query-params]
  (let [inputs (-> query-params
                   (dissoc :active-editor)
                   vals)]
    (when-not (every? clj-str/blank? inputs)
      (-> (uri url)
          (join path)
          (assoc-query* query-params)
          str))))

(defn generate-links
  [{db :db} [_ url data flux fix morph active-editor]]
  (let [max-query-string 1536
        max-url-string 2048
        api-call-params (when flux
                          (merge
                           {:flux flux}
                           (get-used-params flux fix morph data)))
        workflow-params (merge api-call-params
                               (when active-editor
                                 {:active-editor (name active-editor)}))
        api-call-query-string-too-long? (query-string-too-long? api-call-params max-query-string)
        workflow-query-string-too-long? (query-string-too-long? workflow-params max-query-string)
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

;;; Import workflow

(defn- find-filename [files file-extension]
  (->> files
       (mapv :name)
       (keep #(-> (str "(?i).*\\." file-extension)
                  re-pattern
                  (re-matches %)))
       first))

(defn- ->pattern [transformation-type filename]
  (-> (str "\\|(\\s|\\n)*"
           transformation-type
           "(\\s|\\n)*\\(\\s*FLUX_DIR\\s*\\+\\s*\\\""
           filename
           "\\\"\\s*\\)(\\n|\\s)*\\|")
      re-pattern))

(defn- replace-filename [flux files transformation-type]
  (if-let [filename (find-filename files transformation-type)]
    (clj-str/replace flux
                     (->pattern transformation-type filename)
                     (str "|" transformation-type "\n|"))
    flux))

(defn- replace-data-filename [flux files]
  (if-let [data-filename (->> files
                              (mapv :name)
                              (keep #(re-matches #"(?i).*\.(?!morph)(?!fix)(?!flux).*" %))
                              first)]
    (let [pattern (-> (str "FLUX_DIR\\s*\\+\\s*\\\""
                           data-filename
                           "\\\"(\\s|\\n)*\\|\\s*open-file(\\s|\\n)*\\|")
                      re-pattern)]
      (clj-str/replace flux pattern "PG_DATA\n|"))
      flux))

(defn- import-flux->playground-flux [flux files]
  (-> flux
      (replace-filename files "fix")
      (replace-filename files "morph")
      (replace-data-filename files)))

(defn import-editor-content
  [{db :db} [_ files]]
  (let [result (reduce
                    (fn [result {:keys [name content]}]
                      (let [file-extension (re-find #"\.[0-9a-zA-Z]+$" name)]
                        (case file-extension
                          ".flux" (let [flux-content (import-flux->playground-flux content files)]
                                    (cond-> (update result :dispatch-n conj [:dispatch [::edit-input-value :flux flux-content]])
                                      (not= flux-content content) (assoc :message "The flux content has been adapted to work in the playground. Additional adjustments could be necessary.")))
                          ".fix" (update result :dispatch-n concat [[:dispatch [::edit-input-value :fix content]]
                                                        [:dispatch [::switch-editor :fix]]])
                          ".morph" (update result :dispatch-n concat [[:dispatch [::edit-input-value :morph content]]
                                                          [:dispatch [::switch-editor :morph]]])
                          (update result :dispatch-n conj [:dispatch [::edit-input-value :data content]]))))
                    {:dispatch-n []}
                    files)]
     {:db (assoc db :message {:content (concat [(:message result)]
                                               ["Imported workflow with files: "]
                                               (map :name files))
                              :type :info})
      :fx (:dispatch-n result)}))

(re-frame/reg-event-fx
 ::import-editor-content
 import-editor-content)

(defn read-file-list-error
  [{db :db} [_ results]]
  {:db (assoc db :message {:content (concat ["Upload failed for files:"]
                                            (map (fn [{:keys [file error]}]
                                                   (str (g/getValueByKeys file "name") ": " error))
                                                 results))
                           :type :error})})

(re-frame/reg-event-fx
 ::read-file-list-error
 read-file-list-error)

(defn on-read-file-list
  [{db :db} [_ file-list]]
  {:db db
   :fx [[:readfile {:files file-list
                    :charsets "utf-8"
                    :on-success [::import-editor-content]
                    :on-error [::read-file-list-error]}]]})

(re-frame/reg-event-fx
 ::on-read-file-list
 on-read-file-list)

;;; Export workflow

(defn- playground-flux->export-flux [flux data-filename fix-filename morph-filename]
  (-> flux
      (clj-str/replace #"PG_DATA\s*\|" (str "FLUX_DIR + \"" data-filename "\"\n|open-file\n|"))
      (clj-str/replace #"\|\s*fix\s*\|" (str "|fix( FLUX_DIR + \"" fix-filename "\" )\n|"))
      (clj-str/replace #"\|\s*morph\s*\|" (str "|morph( FLUX_DIR + \"" morph-filename "\" ) \n|"))))

(defn export-workflow
  [{:keys [db]} [_ data flux fix morph]]
  (merge {:db (if (every? clj-str/blank? [data flux fix morph])
                (update db :message merge {:content "Nothing to export. All fields are empty."
                                           :type :warning})
                db)}
         (cond-> {}
           (not (clj-str/blank? data)) (update ::effects/export-files conj [data "playground.data"])
           (not (clj-str/blank? flux)) (#(let [flux (playground-flux->export-flux flux "playground.data" "playground.fix" "playground.morph")]
                                         (update % ::effects/export-files conj [flux "playground.flux"])))
           (not (clj-str/blank? fix)) (update ::effects/export-files conj [fix "playground.fix"])
           (not (clj-str/blank? morph)) (update ::effects/export-files conj [morph "playground.morph"]))))

(re-frame/reg-event-fx
 ::export-workflow
 export-workflow)

;;; Processing

(defn process-response
  [{db :db} [_ {:keys [headers body]}]]
  (if-let [content-disposition (:content-disposition headers)]
    (let [file-name (->> content-disposition
                         (re-find #"filename=\"(.*)\"")
                         second)]
      {:db (-> db
               (assoc-in [:result :loading?] false)
               (assoc-in [:result :content] nil))
       ::effects/export-files [[body file-name]]})
    {:db (-> db
             (assoc-in [:result :loading?] false)
             (assoc-in [:result :content] body))}))

(re-frame/reg-event-fx
 ::process-response
 process-response)

(defn- server-problem-message [status status-text body]
  (let [message (get body "message")
        message-content (if message
                          [(str "Response from Server with "
                                "Status-Code \"" status "\" and "
                                "Status-Text \"" status-text "\". ")
                           (str "Exception with Message \"" message "\".")]
                          (str "Response from Server with "
                               "Status-Code \"" status "\" and "
                               "Status-Text \"" status-text \"))]
    (merge {:content message-content}
           (when-let [stacktrace (get body "stacktrace")]
             {:details stacktrace}))))

(defn bad-response
  [{db :db} [_ {:keys [problem problem-message status status-text body]}]]
  (let [body (when body (transit/read (transit/reader :json) body))
        message-data (case problem
                       :fetch {:content (str "Received no server response. Message: " problem-message)}
                       :timeout {:content (str "Response from server: " problem-message)}
                       :body {:content (str "Response from server: " problem-message)}
                       :server (server-problem-message status status-text body))]
       {:db (-> db
                (assoc-in [:result :loading?] false)
                (assoc :message (merge message-data {:type :error})))}))

(re-frame/reg-event-fx
 ::bad-response
 bad-response)

(defn process
  [{db :db} [_ data flux fix morph active-editor]]
  (let [active-editor-in-flux? (when (and active-editor flux)
                                 (re-find (re-pattern (str "\\|(\\s|\\n)*" (name active-editor) "(\\s|\\n)*\\|")) flux))
        message (when (and active-editor (not active-editor-in-flux?))
                  (str "Flux does not use selected " (name active-editor) "."))]
    {:fetch {:method                 :post
             :url                    "process"
             :body                   (.stringify js/JSON (clj->js {:data data
                                                                   :flux flux
                                                                   :fix fix
                                                                   :morph morph}))
             :timeout                10000
             :response-content-types {"text/plain" :text
                                      #"application/.*json" :json}
             :on-success             [::process-response]
             :on-failure             [::bad-response]}
     :db (-> db
             (assoc-in [:result :loading?] true)
             (assoc :message {:content message
                              :type :warning}))}))

(re-frame/reg-event-fx
 ::process
 process)

;;; Initialize-db

(defn examples-response
  [{db :db} [_ {:keys [body]}]]
  (let [body (transit/read (transit/reader :json) body)]
    {:db (assoc db :examples body)}))

(re-frame/reg-event-fx
 ::examples-response
 examples-response)

(defn load-samples
  [{db :db} _]
  {:db db
   :fetch {:method                 :get
           :url                    "examples"
           :timeout                10000
           :response-content-types {#"application/.*json" :json}
           :on-success             [::examples-response]
           :on-failure             [::bad-response]}})

(re-frame/reg-event-fx
 ::load-samples
 load-samples)

(defn versions-response
  [{db :db} [_ {:keys [body]}]]
  (let [body (-> (transit/read (transit/reader :json) body)
                  walk/keywordize-keys)]
    {:db (assoc db :versions body)}))

(re-frame/reg-event-fx
 ::versions-response
 versions-response)

(defn get-backend-versions
  [{db :db} _]
  {:db db
   :fetch {:method                 :get
           :url                    "versions"
           :timeout                10000
           :response-content-types {#"application/.*json" :json}
           :on-success             [::versions-response]
           :on-failure             [::bad-response]}})

(re-frame/reg-event-fx
 ::get-backend-versions
 get-backend-versions)

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
  (let [query-params (utils/parse-url href)]
    (if (empty? query-params)
      {:db (deep-merge
            db/default-db
            (restore-db web-storage)
            {:ui {:height window-height}})
       :fx [[:dispatch [::load-samples]]
            [:dispatch [::get-backend-versions]]]}
      {:db (-> db/default-db
               (assoc-in [:ui :height] window-height))
       :fx (cond->
            (mapv
             (fn [editor]
               [:dispatch [::edit-input-value editor (get query-params editor "")]])
             [:data :flux :fix :morph])
             true (conj [:dispatch [::load-samples]])
             true (conj [:dispatch [::get-backend-versions]])
             (get query-params :active-editor) (conj [:dispatch [::switch-editor (-> query-params :active-editor keyword)]]))
       :storage/set {:session? true
                     :pairs (-> (assoc-query-params {} query-params)
                                generate-pairs)}
       ::effects/unset-url-query-params href})))

(re-frame/reg-event-fx
 ::initialize-db
[(re-frame/inject-cofx :storage/all {:session? true})]
 initialize-db)
