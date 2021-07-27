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
  (events/initialize-db {:dbh {}
                         :event [::events/initialize-db]}))

; Href with query params (data, flux and fix, without processing)
(def href
  "http://metafacture-playground.com/test/?data=1%7Ba%3A%20Faust%2C%20b%20%7Bn%3A%20Goethe%2C%20v%3A%20JW%7D%2C%20c%3A%20Weimar%7D%0A%202%7Ba%3A%20R%C3%A4uber%2C%20b%20%7Bn%3A%20Schiller%2C%20v%3A%20F%7D%2C%20c%3A%20Weimar%7D&fix=map(_id%2C%20id)%0Amap(a%2Ctitle)%0Amap(b.n%2Cauthor)%0A%2F*map(_else)*%2F%0A&flux=as-lines%0A%7Cdecode-formeta%0A%7Cfix%0A%7Cstream-to-xml(rootTag%3D%22collection%22)")

; db with one input-field not empty
(def db1
  (events/edit-value empty-db [:edit-value :data "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}"]))

; db with no empty input-fields
(def db2
  (-> empty-db
      (events/edit-value [:edit-value :data "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}"])
      (events/edit-value [:edit-value :flux "as-lines|decode-formeta|fix|stream-to-xml(rootTag=\"collection\")"])
      (events/edit-value [:edit-value :fix "map(_id, id)\nmap(a,title)\nmap(b.n,author)"])))

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

(deftest update-cursor-position-test
  (testing "Test updating cursor positions."))

(deftest load-sample-test
  (testing "Test loading sample with all fields empty."
    (let [db' (-> empty-db
                  (events/load-sample [:load-sample db/sample-fields])
                  (update :db dissoc :result :links))]
      (is (:db db') (:db db-with-sample))))

  (testing "Test loading sample with part of fields not empty."
    (let [db' (-> db1
                  (events/load-sample [:load-sample db/sample-fields])
                  (update :db dissoc :result :links :storage/set :message)
                  (dissoc :storage/set))]
      (is (= (:db db') (:db db-with-sample)))))

  (testing "Test loading sample with all fields not empty."
    (let [db' (-> db2
                  (events/load-sample [:load-sample db/sample-fields])
                  (update :db dissoc :result :links :storage/set :message))]
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
          {:keys [fix flux data]} (get-in db' [:db :input-fields])
          db'' (events/process db' [:process data flux fix])]
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
          test-url "https://metafacture-playground.com/test/"
          db'' (events/generate-links db' [:generate-links test-url data flux fix])
          api-call-link (uri (get-in db'' [:db :links :api-call]))
          workflow-link (uri (get-in db'' [:db :links :workflow]))]
      (and (is (= (-> api-call-link :query query-string->map :data) data))
           (is (= (-> api-call-link :query query-string->map :flux) flux))
           (is (= (-> api-call-link :query query-string->map :fix) fix))
           (is (= (:path api-call-link) "/test/process"))
           (is (= (-> workflow-link :query query-string->map :data) data))
           (is (= (-> workflow-link :query query-string->map :flux) flux))
           (is (= (-> workflow-link :query query-string->map :fix) fix))
           (is (= (:path workflow-link) "/test/")))))

  (testing "Test not generating links if parameters are too long "
    (let [db' (-> empty-db
                  (events/edit-value [:edit-value :data (generate-random-string 1025)]))
          data (get-in db' [:db :input-fields :data :content])
          db'' (-> db'
                   (events/generate-links [:generate-links "https://metafacture-playground.com/test/" data nil nil])
                   :db)]
      (and (is (= (get-in db'' [:message :content]) "Share links for large workflows are not supported yet"))
           (is (nil? (get-in db'' [:links :api-call])))
           (is (nil? (get-in db'' [:links :workflow]))))))

  (testing "Test not generating links if url is too long "
    (let [db' (-> empty-db
                  (events/load-sample [:load-sample db/sample-fields]))
          data (get-in db' [:db :input-fields :data :content])
          fix (get-in db' [:db :input-fields :fix :content])
          flux (get-in db' [:db :input-fields :flux :content])
          extra-long-test-url (str "https://metafacture-playground.com/" (generate-random-string 1671) "/")
          db'' (-> db'
                   (events/generate-links [:generate-links extra-long-test-url data flux fix])
                   :db)]
      (and (is (= (get-in db'' [:message :content]) "Share links for large workflows are not supported yet"))
           (is (nil? (get-in db'' [:links :api-call])))
           (is (nil? (get-in db'' [:links :workflow])))))))
