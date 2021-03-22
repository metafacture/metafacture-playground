(ns metafacture-playground.event-handler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [metafacture-playground.db :as db]
            [metafacture-playground.events :as events]))

; Initilized db = empty db
(def empty-db
  (events/initialize-db {} [::events/initialize-db]))

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
  {:input-fields db/sample-fields})


(deftest edit-value-test
  (testing "Test editing values."
    (let [new-value "I am a new value"
          db' (-> empty-db 
                  (events/edit-value [:edit-input-value :fix new-value])
                  (update :fields dissoc :result))]
      (is (and (not= db' empty-db)
               (= (get-in db' [:input-fields :fix :content])
                  new-value))))))


(deftest load-sample-test
  (testing "Test loading sample with all fields empty."
    (let [db' (-> empty-db
                  (events/load-sample :load-sample)
                  (dissoc :result))]
         (is db' db-with-sample)))

  (testing "Test loading sample with part of fields not empty."
    (let [db' (-> db1
                  (events/load-sample :load-sample)
                  (dissoc :result))]
      (is (= db' db-with-sample))))

  (testing "Test loading sample with all fields not empty."
    (let [db' (-> db2
                  (events/load-sample :load-sample)
                  (dissoc :result))]
      (is (= db' db-with-sample)))))


(deftest clear-all-test
  (testing "Test clear all fields with all fields already empty."
    (let [db' (events/clear-all empty-db :clear-all)]
      (is (= db' empty-db))))

  (testing "Test clear all fields with part of fields not empty."
    (let [db' (events/clear-all db1 :clear-all)]
      (is (= db' empty-db))))

  (testing "Test clear all fields with all fields not empty."
    (let [db' (events/clear-all db2 :clear-all)]
      (is (= db' empty-db)))))

(deftest process-button-test
  (testing "Test status after processing response"
    (let [db' (-> empty-db
                  (events/load-sample db/sample-fields))
          {:keys [fix flux data]} (get db' :input-fields)
          db'' (:db (events/process {:db db'} [:process data flux fix]))]
      (is (get-in db'' [:result :loading?])))))

(deftest collapse-panel-test
  (testing "Test collapse behaviour"
    (let [db' (-> empty-db
                  (events/collapse-panel [:collapse-panel [:input-fields :flux] false]))]
      (is (and (get-in db' [:input-fields :flux :collapsed?])
               (not (get-in db' [:input-fields :fix :collapsed?]))
               (not (get-in db' [:input-fields :data :collapsed?]))
               (not (get-in db' [:result :collapsed?]))))))
  
  (testing "Test collapsing and expanding a panel"
    (let [db' (-> empty-db
                  (events/collapse-panel [:collapse-panel [:input-fields :flux] false])
                  (events/collapse-panel [:collapse-panel [:input-fields :flux] true]))]
      (is (not (get-in db' [:input-fields :flux :collapsed?]))))))
