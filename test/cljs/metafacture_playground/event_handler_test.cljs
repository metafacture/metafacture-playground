(ns metafacture-playground.event-handler-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [metafacture-playground.db :as db]
            [metafacture-playground.events :as events]))

; Initilized db = empty db
(def empty-db
  (events/initialize-db {} [::events/initialize-db]))

; db with one field not empty
(def db1
  (events/edit-value empty-db [:edit [:fields :data] "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}"]))

; db with no empty fields
(def db2
  (-> empty-db
      (events/edit-value [:edit [:fields :data] "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}"])
      (events/edit-value [:edit [:fields :flux] "as-lines|decode-formeta|fix|stream-to-xml(rootTag=\"collection\")"])
      (events/edit-value [:edit [:fields :fix] "map(_id, id)\nmap(a,title)\nmap(b.n,author)"])))

(def db-with-sample
  {:fields db/sample-fields})


(deftest edit-value-test
  (testing "Test editing values."
    (let [new-value "I am a new value"
          path [:fields :fix]
          db' (-> empty-db 
                  (events/edit-value [:edit path new-value])
                  (update :fields dissoc :result))]
      (is (and (not= db' empty-db)
               (= (get-in db' path)
                  new-value))))))


(deftest load-sample-test
  (testing "Test loading sample with all fields empty."
    (let [db'    (-> empty-db
                     (events/load-sample :load-sample)
                     (update :fields dissoc :result))]
      (is db' db-with-sample)))

  (testing "Test loading sample with part of fields not empty."
    (let [db'    (-> db1
                     (events/load-sample :load-sample)
                     (update :fields dissoc :result))]
      (is (= db' db-with-sample))))

  (testing "Test loading sample with all fields not empty."
    (let [db' (-> db2
                  (events/load-sample :load-sample)
                  (update :fields dissoc :result))]
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
