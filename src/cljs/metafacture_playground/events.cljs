(ns metafacture-playground.events
  (:require
   [metafacture-playground.db :as db]
   [metafacture-playground.effects :as effects]
   [metafacture-playground.utils :as utils]
   [re-frame.core :as re-frame]
   [day8.re-frame.fetch-fx]
   [jtk-dvlp.re-frame.readfile-fx]
   [lambdaisland.uri :refer [uri join assoc-query*]]
   [com.degel.re-frame.storage]
   [clojure.string :as clj-str]
   [clojure.walk :as walk]
   [cognitect.transit :as transit]
   [goog.object :as g]
   [cljs.pprint :as cljs-pprint]))

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
        width (if (> max-row-length visible-chars)
                16
                (or (get-in db [:editors editor :default-width])
                    (get-in db [:editors editor :width])))]
      {:db (cond->
            (assoc-in db [:editors editor :width] width)
             (= editor :flux) (assoc-in [:editors :transformation :width] width)
             (= editor :transformation) (assoc-in [:editors :flux :width] width))
       :storage/set {:session? true
                     :pairs (cond->
                             [{:name (->storage-key [:editors editor :width])
                              :value width}]
                              (= editor :flux) (conj {:name (->storage-key [:editors :transformation :width])
                                                      :value width})
                              (= editor :transformation) (conj {:name (->storage-key [:editors :flux :width])
                                                                :value width}))}}))

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
  [{:keys [db]} [_ editor status]]
  (let [db-path [:editors editor :collapsed?]
        new-value (not status)]
    {:db (assoc-in db db-path new-value)
     :storage/set {:session? true
                   :name (->storage-key db-path) :value new-value}}))

(re-frame/reg-event-fx
 ::collapse-panel
 collapse-panel)

;;; Editing input fields

(defn- disable-editor? [db editor content]
  (let [variable (get-in db [:editors editor :file-variable])]
    (-> variable
        re-pattern
        (re-find content)
        boolean
        not)))

(defn edit-editor-content
  [{db :db} [_ editor new-value & [triggered-by-code?]]]
  (let [disable-editors (when (= editor :flux)
                          (mapv (fn [editor]
                                  [editor (disable-editor? db editor new-value)])
                                [:data :transformation]))]
    (merge
     {:db (cond-> (reduce
                   (fn [db [editor v]]
                     (assoc-in db [:editors editor :disabled?] v))
                   db
                   disable-editors)
            true (assoc-in [:editors editor :content] new-value)
            triggered-by-code? (update-in [:editors editor :key-count] inc)
            (not triggered-by-code?) (assoc-in [:ui :dropdown :active-item] nil))
      :storage/set {:session? true
                    :pairs (conj
                            (mapv
                             (fn [[editor v]]
                               {:name (->storage-key [:editors editor :disabled?]) :value v})
                             disable-editors)
                            {:name (->storage-key [:editors editor :content]) :value new-value}
                            (when-not triggered-by-code? {:name (->storage-key [:ui :dropdown :active-item]) :value nil}))}}
     (when-not (= editor :result)
       {:dispatch [::update-width editor new-value]}))))

(re-frame/reg-event-fx
 ::edit-editor-content
 edit-editor-content)

(defn open-dropdown
  [{db :db} [_ folder status]]
  {:db (assoc-in db [:ui :dropdown folder :open?] status)})

(re-frame/reg-event-fx
 ::open-dropdown
 open-dropdown)

(defn- find-example-data [example-name data]
  (some #(cond
           (and (map? (val %))
                (= (-> % key utils/display-name) example-name))
           (val %)

           (and (map? (val %))
                (not-any? #{:data :flux :fix :morph} (keys (val %))))
           (find-example-data example-name (val %))

           :else false)
        data))

(defn load-example
  [{db :db} [_ example-name]]
  (let [example-data (find-example-data example-name (:examples db))]
    (if example-data
      {:db (-> db
               (assoc :result nil)
               (assoc-in [:ui :dropdown :active-item] example-name))
       :fx (mapv
             (fn [editor]
               [:dispatch [::edit-editor-content editor (get example-data editor "") true]])
             [:data :flux :transformation])
       :storage/set {:session? true
                     :name (->storage-key [:ui :dropdown :active-item])
                     :value example-name}
       ::effects/set-url-query-params example-name}
      {:db (assoc db :message {:content (str "Could not find example with name \"" example-name "\".")
                               :type :warning})
       ::effects/unset-url-query-params nil})))

(re-frame/reg-event-fx
  ::load-example
  load-example)

;;; Copy to clipboard

(defn copy-link
  [{:keys [db]} [_ val]]
  {:db db
   ::effects/copy-to-clipboard val})

(re-frame/reg-event-fx
 ::copy-link
 copy-link)

;;; Share links

(defn- variable-used? [content variable]
  (re-find (re-pattern variable) content))

(defn- get-used-params [params]
  (if-let [flux (get-in params [:flux :content])]
    (cond-> {:flux flux}
      (variable-used? flux (get-in params [:transformation :variable])) (merge {:transformation (get-in params [:transformation :content])})
      (variable-used? flux (get-in params [:data :variable])) (merge {:data (get-in params [:data :content])}))
    {}))

(def max-url-string 65536) ; maximum displayable URL length in Firefox

(defn- url-too-long? [url]
  (if-not (:error url)
    (> (count url) max-url-string)
    true))

(defn generate-link [url path query-params]
  (let [inputs (vals query-params)]
    (when-not (every? clj-str/blank? inputs)
      (try
        (-> (uri url)
            (join path)
            (assoc-query* query-params)
            str)
        (catch js/Error _
          {:error true})))))

(defn add-error-message [result name url]
  (if (:error url)
    (conj result (str "It occured an JS Error generating the " name"."))
    (conj result (str "The " name " is "
                      (- max-url-string (count url))
                      " characters too long (Maximum: " max-url-string ")"))))

(defn firefox-used? []
  (> (-> js/navigator .-userAgent (.indexOf "Firefox"))
     -1))

(defn generate-links
  [{db :db} [_ uri uri-params]]
  (let [api-call-params (get-used-params uri-params)
        workflow-params (merge api-call-params
                               (when-let [active-editor (get uri-params :active-editor)]
                                 {:active-editor (name active-editor)}))
        api-call-link (when api-call-params (generate-link uri "./process" api-call-params))
        workflow-link (when workflow-params (generate-link uri "" workflow-params))
        api-call-link-too-long? (url-too-long? api-call-link)
        workflow-link-too-long? (url-too-long? workflow-link)
        message (when (or api-call-link-too-long? workflow-link-too-long?)
                  {:content (cond-> ["There was a problem generating the share links:"]
                              api-call-link-too-long? (add-error-message "api call link" api-call-link)
                              workflow-link-too-long? (add-error-message "workflow link" workflow-link)
                              (not (firefox-used?)) (conj "Consider using Firefox."))
                   :type :warning})]
    {:db (-> db
             (assoc :message message)
             (assoc-in [:links :api-call] (when-not api-call-link-too-long? api-call-link))
             (assoc-in [:links :workflow] (when-not workflow-link-too-long? workflow-link)))}))

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
  (let [triggered-by-button? true
        result (reduce
                (fn [result {:keys [name content]}]
                  (let [file-extension (re-find #"\.[0-9a-zA-Z]+$" name)]
                    (case file-extension
                      ".flux" (let [flux-content (import-flux->playground-flux content files)]
                                (cond-> (update result :fx conj [:dispatch [::edit-editor-content :flux flux-content triggered-by-button?]])
                                  (not= flux-content content) (assoc :message "The flux content has been adapted to work in the playground. Additional adjustments could be necessary.")))
                      ".fix" (update result :fx concat [[:dispatch [::edit-editor-content :fix content triggered-by-button?]]
                                                        [:dispatch [::switch-editor :fix]]])
                      ".morph" (update result :fx concat [[:dispatch [::edit-editor-content :morph content triggered-by-button?]]
                                                          [:dispatch [::switch-editor :morph]]])
                      (update result :fx conj [:dispatch [::edit-editor-content :data content triggered-by-button?]]))))
                {:fx []}
                files)]
     {:db (assoc db :message {:content (concat [(:message result)]
                                               ["Imported workflow with files: "]
                                               (map :name files))
                              :type :info})
      :fx (:fx result)}))

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
  [{db :db} [_ data flux transformation]]
    {:fetch {:method                 :post
             :url                    "process"
             :body                   (.stringify js/JSON (clj->js {:data data
                                                                   :flux flux
                                                                   :transformation transformation}))
             :timeout                100000
             :response-content-types {"text/plain" :text
                                      #"application/.*json" :json}
             :on-success             [::process-response]
             :on-failure             [::bad-response]}
     :db (assoc-in db [:result :loading?] true)})

(re-frame/reg-event-fx
 ::process
 process)

;;; Initialize-db

(defn- links->examples []
  (map
   (fn [[k v]]
     (if (map? v)
       {k (into (sorted-map)
                (links->examples)
                v)}
       {(utils/display-name k) (utils/parse-url v)}))))

(defn examples-response
  [{db :db} [_ initial-example {:keys [body]}]]
  (let [body (transit/read (transit/reader :json) body)]
    (if initial-example
      {:db (assoc db :examples (into (sorted-map)
                                     (links->examples)
                                     body))
       :fx [[:dispatch [::load-example initial-example]]]}
      {:db (assoc db :examples (into (sorted-map)
                                     (links->examples)
                                     body))})))

(re-frame/reg-event-fx
 ::examples-response
 examples-response)

(defn load-examples
  [{db :db} [_ initial-example]]
  {:db db
   :fetch {:method                 :get
           :url                    "examples"
           :timeout                10000
           :response-content-types {#"application/.*json" :json}
           :on-success             [::examples-response initial-example]
           :on-failure             [::bad-response]}})

(re-frame/reg-event-fx
 ::load-examples
 load-examples)

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

(defn- assoc-query-params [start-db {:keys [data flux transformation]}]
  (cond-> start-db
    data (assoc-in [:editors :data :content] data)
    flux (assoc-in [:editors :flux :content] flux)
    transformation (assoc-in [:editors :morph :content] transformation)))

(defn initialize-db
  [{[_ href window-height] :event
    web-storage :storage/all}]
  (let [query-params (utils/parse-url href)
        fx [[:dispatch [::get-backend-versions]]]]
    (cond
      (empty? query-params) {:db (deep-merge
                                  db/default-db
                                  (restore-db web-storage)
                                  {:ui {:height window-height}})
                             :fx (conj fx [:dispatch [::load-examples]])}
      (:example query-params) {:db (-> db/default-db
                                       (assoc-in [:ui :height] window-height))
                               :fx (conj fx [:dispatch [::load-examples (get query-params :example)]])
                               :storage/set {:session? true
                                             :pairs (-> (assoc-query-params {} query-params)
                                                        generate-pairs)}}
      :else {:db (-> db/default-db
                     (assoc-in [:ui :height] window-height))
             :fx (concat fx
                     [[:dispatch [::load-examples]]]
                     (mapv
                      (fn [editor]
                        [:dispatch [::edit-editor-content editor (get query-params editor "")]])
                      [:data :flux :transformation]))
             :storage/set {:session? true
                           :pairs (-> (assoc-query-params {} query-params)
                                      generate-pairs)}
             ::effects/unset-url-query-params href})))

(re-frame/reg-event-fx
 ::initialize-db
[(re-frame/inject-cofx :storage/all {:session? true})]
 initialize-db)
