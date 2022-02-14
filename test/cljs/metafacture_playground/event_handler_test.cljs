(ns metafacture-playground.event-handler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [re-frame.core :as re-frame]
            [day8.re-frame.test :as rf-test]
            [metafacture-playground.db :as db]
            [metafacture-playground.events :as events]
            [metafacture-playground.subs :as subs]
            [metafacture-playground.utils :as utils]
            [lambdaisland.uri :refer [uri query-string->map]]
            [shadow.resource :as rc]))

; Utils

(defn- generate-random-string [length]
  (->> (repeat length \a)
       (apply str)))

(def sample-data (-> (rc/inline "examples/Local_formeta_to_XML_(fix)")
                     utils/parse-url))

; Initilized db = empty db
(def empty-db
  (-> (events/initialize-db {:db {}
                             :event [::events/initialize-db]})
      (dissoc :dispatch :fx :storage/set :metafacture-playground.effects/unset-url-query-params)))

; db with one input-field not empty
(def db1
  (events/edit-value empty-db [:edit-value :data (:data sample-data)]))

; db with no empty input-fields
(def db2
  (-> empty-db
      (events/edit-value [:edit-value :data (:data sample-data)])
      (events/edit-value [:edit-value :flux (:flux sample-data)])
      (events/edit-value [:edit-value :fix  (:fix sample-data)])))

(deftest initialize-db
  (testing "Test initializing of db without values"
    (is (= empty-db {:db db/default-db})))

  (testing "Test initializing of db with values"
    (rf-test/run-test-sync
     (let [href "/playground/?data=1%7Ba%3A+Faust%2C+b+%7Bn%3A+Goethe%2C+v%3A+JW%7D%2C+c%3A+Weimar%7D%0A2%7Ba%3A+R%C3%A4uber%2C+b+%7Bn%3A+Schiller%2C+v%3A+F%7D%2C+c%3A+Weimar%7D&flux=as-lines%0A%7Cdecode-formeta%0A%7Cfix%0A%7Cencode-xml%28rootTag%3D%22collection%22%29&fix=move_field%28_id%2C+id%29%0Amove_field%28a%2C+title%29%0Apaste%28author%2C+b.v%2C+b.n%2C+%27~aus%27%2C+c%29%0Aretain%28id%2C+title%2C+author%29"]
       (re-frame/dispatch [::events/initialize-db href])
       (and (is @(re-frame/subscribe [::subs/field-value :data]))
            (is @(re-frame/subscribe [::subs/field-value :flux]))
            (is @(re-frame/subscribe [::subs/field-value :fix]))
            (is (not @(re-frame/subscribe [::subs/process-result])))
            (is (not @(re-frame/subscribe [::subs/link :api-call])))
            (is (not @(re-frame/subscribe [::subs/link :workflow]))))))))

(deftest edit-value-test
  (testing "Test editing values."
    (let [new-value "I am a new value"
          db' (-> empty-db
                  (events/edit-value [:edit-input-value :fix new-value])
                  (update-in [:db :input-fields] dissoc :result)
                  (dissoc :storage/set))]
      (and (is (not= db' empty-db))
           (is (= (get-in db' [:db :input-fields :fix :content])
                  new-value))
           (is (true? (get-in db' [:db :input-fields :data :disabled?])))
           (is (true? (get-in db' [:db :input-fields :fix :disabled?])))
           (is (true? (get-in db' [:db :input-fields :morph :disabled?]))))))
  
(testing "Test disabling editor depending on editing values")
  (let [new-value "I use the input PG_DATA and a | morph | "
        db' (-> empty-db
                (events/edit-value [:edit-input-value :flux new-value])
                (update-in [:db :input-fields] dissoc :result)
                (dissoc :storage/set))]
    (and (is (not= db' empty-db))
         (is (= (get-in db' [:db :input-fields :flux :content])
                new-value))
         (is (false? (get-in db' [:db :input-fields :data :disabled?])))
         (is (true? (get-in db' [:db :input-fields :fix :disabled?])))
         (is (false? (get-in db' [:db :input-fields :morph :disabled?]))))))

(deftest load-sample-test
  (testing "Test loading sample"
    (rf-test/run-test-sync
     (re-frame/dispatch [::events/load-sample "sample data" sample-data])
     (and (is (= @(re-frame/subscribe [::subs/field-value :data])
                 (:data sample-data)))
          (is (= @(re-frame/subscribe [::subs/field-value :flux])
                 (:flux sample-data)))
          (is (= @(re-frame/subscribe [::subs/field-value :fix])
                 (:fix sample-data)))
          (is (not @(re-frame/subscribe [::subs/dropdown-open?])))
          (is (= @(re-frame/subscribe [::subs/dropdown-active-item])
                 "sample data"))))))


(deftest clear-all-test
  (testing "Test clear all fields with all fields already empty."
    (let [db' (events/clear-all empty-db :clear-all)]
      (is (= (:db db') (:db empty-db)))))

  (testing "Test clear all fields with part of fields not empty."
    (let [db' (events/clear-all db1 :clear-all)]
      (is (= (:db db') (:db empty-db)))))

  (testing "Test clear all fields with all fields not empty."
    (let [db' (events/clear-all db2 :clear-all)]
      (is (= (:db db') (:db empty-db))))))


(deftest process-button-test
  (testing "Test status after processing response"
    (let [db' (-> empty-db
                  (events/load-sample [:load-sample sample-data]))
          {:keys [fix flux data morph]} (get-in db' [:db :input-fields])
          db'' (events/process db' [:process (:content data) (:content flux) (:content fix) (:content morph) :fix])]
      (is (get-in db'' [:db :result :loading?])))))

(deftest collapse-panel-test
  (testing "Test collapse behaviour"
    (let [db' (-> empty-db
                  (events/collapse-panel [:collapse-panel [:input-fields :flux] false]))]
      (and (is (get-in db' [:db :input-fields :flux :collapsed?]))
           (is (not (get-in db' [:db :input-fields :fix :collapsed?])))
           (is (not (get-in db' [:db :input-fields :data :collapsed?])))
           (is (not (get-in db' [:db :result :collapsed?]))))))

  (testing "Test collapsing and expanding a panel"
    (let [db' (-> empty-db
                  (events/collapse-panel [:collapse-panel [:input-fields :flux] false])
                  (events/collapse-panel [:collapse-panel [:input-fields :flux] true]))]
      (is (not (get-in db' [:db :input-fields :flux :collapsed?]))))))

(deftest message-test
  (testing "Test dismissing message"
    (let [db' (-> empty-db
                  (assoc-in [:db :message :content] "I am a new error")
                  (assoc-in [:db :message :type] :error)
                  (events/dismiss-message [:dismiss-message]))]
      (is (nil? (get-in db' [:db :message])))))

  (testing "Show and hide error details"
    (let [db' (-> empty-db
                  (assoc-in [:db :message :content] "I am a new error with details")
                  (assoc-in [:db :message :type] :error)
                  (assoc-in [:db :message :details] "You clicked too fast!"))
          db'' (-> db'
                   (events/show-error-details [:show-error-details true]))
          db''' (-> db''
                    (events/show-error-details [:show-error-details false]))]
      (and (is (= (get-in db' [:db :message :show-details?]) false))
           (is (= (get-in db'' [:db :message :show-details?]) true))
           (is (= (get-in db''' [:db :message :show-details?]) false))))))

(deftest generate-links-test
  (testing "Test generating share links"
    (let [db' (-> empty-db
                  (events/edit-value [:edit-value :data (:data sample-data)])
                  (events/edit-value [:edit-value :fix (:fix sample-data)])
                  (events/edit-value [:edit-value :flux (:flux sample-data)]))
          data (get-in db' [:db :input-fields :data :content])
          fix (get-in db' [:db :input-fields :fix :content])
          flux (get-in db' [:db :input-fields :flux :content])
          morph (get-in db' [:db :input-fields :morph :content])
          test-url "http://test.metafacture.org/playground/"
          db'' (events/generate-links db' [:generate-links test-url data flux fix morph :fix])
          api-call-link (uri (get-in db'' [:db :links :api-call]))
          workflow-link (uri (get-in db'' [:db :links :workflow]))]
      (and (is (= (-> api-call-link :query query-string->map :data) data))
           (is (= (-> api-call-link :query query-string->map :flux) flux))
           (is (= (-> api-call-link :query query-string->map :fix) fix))
           (is (= (:path api-call-link) "/playground/process"))
           (is (= (-> workflow-link :query query-string->map :data) data))
           (is (= (-> workflow-link :query query-string->map :flux) flux))
           (is (= (-> workflow-link :query query-string->map :fix) fix))
           (is (= (-> workflow-link :query query-string->map :morph) morph))
           (is (= (:path workflow-link) "/playground/")))))

  (testing "Test not generating links if parameters are too long"
    (let [db' (-> empty-db
                  (events/edit-value [:edit-value :flux (generate-random-string 1537)]))
          flux (get-in db' [:db :input-fields :flux :content])
          db'' (-> db'
                   (events/generate-links [:generate-links "http://test.metafacture.org/playground/" nil flux nil nil :fix])
                   :db)]
      (and (is (= (get-in db'' [:message :content]) "Share links for large workflows are not supported yet"))
           (is (nil? (get-in db'' [:links :api-call])))
           (is (nil? (get-in db'' [:links :workflow]))))))

  (testing "Test not generating links if url is too long"
    (let [extra-long-test-url (str "http://test.metafacture.org/playground/" (generate-random-string 1671) "/")
          db'' (-> empty-db
                   (events/generate-links [:generate-links extra-long-test-url (:data sample-data) (:flux sample-data) (:fix sample-data) "" :fix])
                   :db)]
      (and (is (= (get-in db'' [:message :content]) "Share links for large workflows are not supported yet"))
           (is (nil? (get-in db'' [:links :api-call])))
           (is (nil? (get-in db'' [:links :workflow])))))))

(deftest update-widths-test
  (testing "Test updating the width of flux editor"
    (let [long-flux-content "as-lines\n
                             |decode-formeta\n
                             |fix\n
                             |encode-xml (rootTag= \"collection25481354555465645645654\")"
          db' (-> empty-db
                  (events/update-width [:update-width :flux long-flux-content]))]
      (and (is (= (get-in db' [:db :input-fields :flux :width]) 16))
           (is (nil? (get-in db' [:input-fields :switch :width])))))

    (testing "Test updating the width of fix editor"
      (let [long-fix-content "move_field(_id, id)\n
                              move_field(a,title)\n
                              move_field(b.n,authooooooooooooooooooooooooooooooooooor)\n
                              /*vacuum()*/"
            db' (-> empty-db
                    (events/update-width [:update-width :fix long-fix-content]))]
        (and (is (= (get-in db' [:db :input-fields :switch :width]) 16))
             (is (nil? (get-in db' [:db :input-fields :flux :width]))))))
    
(testing "Test updating the width of fix editor"
  (let [long-flux-content "as-lines\n
                             |decode-formeta\n
                             |fix\n
                             |encode-xml (rootTag= \"collection25481354555465645656454\")"
        long-fix-content "map(_id, id)\n
                              move_field(a,title)\n
                              move_field(b.n,authooooooooooooooooooooooooooooooooooor)\n
                              /*vacuum()*/"
        db' (-> empty-db
                (events/update-width [:update-width :flux long-flux-content])
                (events/update-width [:update-width :fix long-fix-content]))]
    (and (is (= (get-in db' [:db :input-fields :switch :width]) 16))
         (is (= (get-in db' [:db :input-fields :flux :width]) 16)))))))

(deftest switch-editor-test
  (testing "Test switching between fix and morph editor"
    (let [db' (-> empty-db
                  (events/switch-editor [:switch-editor :morph]))]
      (is (= (get-in db' [:db :input-fields :switch :active]) :morph)))))
