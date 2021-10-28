(ns metafacture-playground.event-handler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [metafacture-playground.db :as db]
            [metafacture-playground.events :as events]
            [lambdaisland.uri :refer [uri query-string->map]]))

; Utils

(defn- generate-random-string [length]
  (->> (repeat length \a)
       (apply str)))

; Initilized db = empty db
(def empty-db
  (events/initialize-db {:db {}
                         :event [::events/initialize-db]}))

; Href with query params (data, flux and fix, without processing)
(def href
  "http://test.metafacture.org/playground/?data=1%7Ba%3A+Faust%2C+b+%7Bn%3A+Goethe%2C+v%3A+JW%7D%2C+c%3A+Weimar%7D%0A2%7Ba%3A+R%C3%A4uber%2C+b+%7Bn%3A+Schiller%2C+v%3A+F%7D%2C+c%3A+Weimar%7D&flux=as-lines%0A%7Cdecode-formeta%0A%7Cfix%0A%7Cencode-xml%28rootTag%3D%22collection%22%29&fix=move_field%28_id%2C+id%29%0Amove_field%28a%2C+title%29%0Apaste%28author%2C+b.v%2C+b.n%2C+%27~aus%27%2C+c%29%0Aretain%28id%2C+title%2C+author%29")

; db with one input-field not empty
(def db1
  (events/edit-value empty-db [:edit-value :data (:data db/sample-data)]))

; db with no empty input-fields
(def db2
  (-> empty-db
      (events/edit-value [:edit-value :data (:data db/sample-data)])
      (events/edit-value [:edit-value :flux (:flux-with-fix db/sample-data)])
      (events/edit-value [:edit-value :fix  (:ix db/sample-data)])))

(def db-with-sample
  {:db {:input-fields db/sample-fields}})

(deftest initialize-db
  (testing "Test initializing of db without values"
    (is (= empty-db {:db db/default-db})))

  (testing "Test initializing of db with values"
    (let [initialized-db (:db (events/initialize-db {:event [::events/initialize-db href]}))]
      (and (is (get-in initialized-db [:input-fields :data :content]))
           (is (get-in initialized-db [:input-fields :flux :content]))
           (is (get-in initialized-db [:input-fields :fix :content]))
           (is (not (get-in initialized-db [:result :content])))
           (is (not (get-in initialized-db [:links :api-call])))
           (is (not (get-in initialized-db [:links :workflow])))
           (is (not (get-in initialized-db [:links :processed-workflow])))))))

(deftest edit-value-test
  (testing "Test editing values."
    (let [new-value "I am a new value"
          db' (-> empty-db
                  (events/edit-value [:edit-input-value :fix new-value])
                  (update-in [:db :input-fields] dissoc :result)
                  (dissoc :storage/set))]
      (and (is (not= db' empty-db))
           (is (= (get-in db' [:db :input-fields :fix :content])
                  new-value))))))

(deftest load-sample-test
  (testing "Test loading sample with all fields empty."
    (let [db' (-> empty-db
                  (events/load-sample [:load-sample db/sample-fields])
                  (update :db dissoc :result :links))]
      (is (:db db') (:db db-with-sample))))

  (testing "Test loading sample with part of fields not empty."
    (let [db' (-> db1
                  (events/load-sample [:load-sample db/sample-fields])
                  (update :db dissoc :result :links :storage/set :message :ui)
                  (dissoc :storage/set))]
      (is (= (:db db') (:db db-with-sample)))))

  (testing "Test loading sample with all fields not empty."
    (let [db' (-> db2
                  (events/load-sample [:load-sample db/sample-fields])
                  (update :db dissoc :result :links :storage/set :message :ui))]
      (is (= (:db db') (:db db-with-sample))))))


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
                  (events/load-sample [:load-sample db/sample-fields]))
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

(deftest generate-links-test
  (testing "Test generating share links"
    (let [db' (-> empty-db
                  (events/load-sample [:load-sample db/sample-fields]))
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
                  (events/edit-value [:edit-value :data (generate-random-string 1537)]))
          data (get-in db' [:db :input-fields :data :content])
          db'' (-> db'
                   (events/generate-links [:generate-links "http://test.metafacture.org/playground/" data nil nil nil :fix])
                   :db)]
      (and (is (= (get-in db'' [:message :content]) "Share links for large workflows are not supported yet"))
           (is (nil? (get-in db'' [:links :api-call])))
           (is (nil? (get-in db'' [:links :workflow]))))))

  (testing "Test not generating links if url is too long"
    (let [db' (-> empty-db
                  (events/load-sample [:load-sample db/sample-fields]))
          data (get-in db' [:db :input-fields :data :content])
          fix (get-in db' [:db :input-fields :fix :content])
          morph (get-in db' [:db :input-fields :morph :content])
          flux (get-in db' [:db :input-fields :flux :content])
          extra-long-test-url (str "http://test.metafacture.org/playground/" (generate-random-string 1671) "/")
          db'' (-> db'
                   (events/generate-links [:generate-links extra-long-test-url data flux fix morph :fix])
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
