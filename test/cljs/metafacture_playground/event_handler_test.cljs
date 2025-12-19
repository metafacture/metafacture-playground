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

(def example-name "Local JSON to XML (fix)")

(def example-data (-> (rc/inline "examples/Local+JSON+to+XML+(fix)")
                     utils/parse-url))

; Initilized db = empty db
(def empty-db
  (-> (events/initialize-db {:db {}
                             :event [::events/initialize-db]})
      (dissoc :dispatch :fx :storage/set :metafacture-playground.effects/unset-url-query-params)))

(deftest initialize-db
  (testing "Test initializing of db without values"
    (is (= empty-db {:db db/default-db})))

  (testing "Test initializing of db with values"
    (rf-test/run-test-sync
     (let [href "/playground/?flux=inputFile%0A%7Cas-lines%0A%7Cdecode-formeta%0A%7Cfix%28transformationFile%29%0A%7Cencode-xml%28rootTag%3D%22collection%22%29%0A%7Cprint%0A%3B&transformation=move_field%28_id%2C+id%29%0Amove_field%28a%2C+title%29%0Apaste%28author%2C+b.v%2C+b.n%2C+%27~aus%27%2C+c%29%0Aretain%28id%2C+title%2C+author%29&data=1%7Ba%3A+Faust%2C+b+%7Bn%3A+Goethe%2C+v%3A+JW%7D%2C+c%3A+Weimar%7D%0A2%7Ba%3A+R%C3%A4uber%2C+b+%7Bn%3A+Schiller%2C+v%3A+F%7D%2C+c%3A+Weimar%7D"]
       (re-frame/dispatch [::events/initialize-db href])
       (and (is @(re-frame/subscribe [::subs/editor-content :data]))
            (is @(re-frame/subscribe [::subs/editor-content :flux]))
            (is @(re-frame/subscribe [::subs/editor-content :transformation]))
            (is (= "" @(re-frame/subscribe [::subs/editor-content :result])))
            (is (not @(re-frame/subscribe [::subs/link :api-call])))
            (is (not @(re-frame/subscribe [::subs/link :workflow]))))))))

(deftest edit-value-test
  (testing "Test editing values by button click."
    (let [new-value "I am a new value"
          db' (-> empty-db
                  (events/edit-editor-content [:edit-editor-content :transformation new-value :example])
                  (update-in [:db :editors] dissoc :result)
                  (dissoc :storage/set))]
      (and (is (not= db' empty-db))
           (is (= (get-in db' [:db :editors :transformation :content])
                  new-value))
           (is (true? (get-in db' [:db :editors :data :disabled?])))
           (is (true? (get-in db' [:db :editors :transformation :disabled?]))))))
  
  
  (testing "Test editing values in editor."
    (let [new-value "I am a new value"
          db' (-> empty-db
                  (events/edit-editor-content [:edit-editor-content :transformation new-value])
                  (update-in [:db :editors] dissoc :result)
                  (dissoc :storage/set))]
      (and (is (not= db' empty-db))
           (is (= (get-in db' [:db :editors :transformation :shadow-content])
                  new-value))
           (is (true? (get-in db' [:db :editors :data :disabled?])))
           (is (true? (get-in db' [:db :editors :transformation :disabled?]))))))

  
(testing "Test disabling editor depending on editing values")
  (let [new-value "I only use the inputFile"
        db' (-> empty-db
                (events/edit-editor-content [:edit-editor-content :flux new-value :examples])
                (update-in [:db :editors] dissoc :result)
                (dissoc :storage/set))]
    (and (is (not= db' empty-db))
         (is (= (get-in db' [:db :editors :flux :content])
                new-value))
         (is (false? (get-in db' [:db :editors :data :disabled?])))
         (is (true? (get-in db' [:db :editors :transformation :disabled?]))))))

(defn test-fixtures
  []
  (re-frame/reg-event-fx
   ::test-fixtures
   (fn [cofx _]
     (-> cofx
         (assoc :db db/default-db)
         (assoc-in [:db :examples] {example-name example-data})))))

(deftest load-example-test
  (testing "Test loading example"
    (rf-test/run-test-sync
     (test-fixtures)
     (re-frame/dispatch [::test-fixtures])
     (re-frame/dispatch [::events/load-example example-name])
     (and (is (= @(re-frame/subscribe [::subs/editor-content :data])
                 (:data example-data)))
          (is (= @(re-frame/subscribe [::subs/editor-content :flux])
                 (:flux example-data)))
          (is (= @(re-frame/subscribe [::subs/editor-content :transformation])
                 (:transformation example-data)))
          (is (not @(re-frame/subscribe [::subs/dropdown-open? "main"])))
          (is (= @(re-frame/subscribe [::subs/dropdown-active-item])
                 example-name))))))

(deftest process-button-test
  (testing "Test status after processing response"
    (let [db' (-> empty-db
                  (events/load-example [:load-example example-name]))
          {:keys [data flux transformation]} (get-in db' [:db :editors])
          db'' (events/process db' [:process (:content data) (:content flux) (:content transformation)])]
      (is (get-in db'' [:db :editors :result :loading?])))))

(deftest collapse-panel-test
  (testing "Test collapse behaviour"
    (let [db' (-> empty-db
                  (events/collapse-panel [:collapse-panel :flux false]))]
      (and (is (get-in db' [:db :editors :flux :collapsed?]))
           (is (not (get-in db' [:db :editors :transformation :collapsed?])))
           (is (not (get-in db' [:db :editors :data :collapsed?])))
           (is (not (get-in db' [:db :editors :result :collapsed?]))))))

  (testing "Test collapsing and expanding a panel"
    (let [db' (-> empty-db
                  (events/collapse-panel [:collapse-panel :flux false])
                  (events/collapse-panel [:collapse-panel :flux true]))]
      (is (not (get-in db' [:db :editors :flux :collapsed?]))))))

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
                  (events/edit-editor-content [:edit-editor-content :data (:data example-data) :other])
                  (events/edit-editor-content [:edit-editor-content :transformation (:transformation example-data) :other])
                  (events/edit-editor-content [:edit-editor-content :flux (:flux example-data) :other]))
          data (get-in db' [:db :editors :data :content])
          flux (get-in db' [:db :editors :flux :content])
          transformation (get-in db' [:db :editors :transformation :shadow-content])
          test-url "http://test.metafacture.org/playground/"
          db'' (events/generate-links db' [:generate-links test-url {:data {:content data
                                                                            :variable (get-in db/default-db [:editors :data :file-variable])}
                                                                     :flux {:content flux
                                                                            :variable (get-in db/default-db [:editors :flux :file-variable])}
                                                                     :transformation {:content transformation
                                                                                      :variable (get-in db/default-db [:editors :transformation :file-variable])}}])
          api-call-link (uri (get-in db'' [:db :links :api-call]))
          workflow-link (uri (get-in db'' [:db :links :workflow]))]
      (and (is (= (-> api-call-link :query query-string->map :data) data))
           (is (= (-> api-call-link :query query-string->map :flux) flux))
           (is (= (-> api-call-link :query query-string->map :transformation) transformation))
           (is (= (:path api-call-link) "/playground/process"))
           (is (= (-> workflow-link :query query-string->map :data) data))
           (is (= (-> workflow-link :query query-string->map :flux) flux))
           (is (= (-> workflow-link :query query-string->map :transformation) transformation))
           (is (= (:path workflow-link) "/playground/")))))

  (testing "Test not generating links if url is too long"
    (let [extra-long-test-url (str "http://test.metafacture.org/playground/" (generate-random-string 66000) "/")
          db'' (-> empty-db
                   (events/generate-links [:generate-links extra-long-test-url {:data {:content (:data example-data)
                                                                                       :variable (get-in db/default-db [:editors :data :file-variable])}
                                                                                :flux {:content (:flux example-data)
                                                                                       :variable (get-in db/default-db [:editors :flux :file-variable])}
                                                                                :transformation {:content (:transformation example-data)
                                                                                                 :variable (get-in db/default-db [:editors :transformation :file-variable])}}])
                   :db)]
      (and (is (get-in db'' [:message :content]))
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
      (and (is (= (get-in db' [:db :editors :flux :width]) 16))
           (is (= (get-in db' [:db :editors :transformation :width]) 16))
           (is (= (get-in db' [:db :editors :data :width]) 16))))

    (testing "Test updating the width of fix editor"
      (let [long-fix-content "move_field(_id, id)\n
                              move_field(a,title)\n
                              move_field(b.n,authooooooooooooooooooooooooooooooooooor)\n
                              /*vacuum()*/"
            db' (-> empty-db
                    (events/update-width [:update-width :fix long-fix-content]))]
        (and (is (= (get-in db' [:db :editors :transformation :width]) 8))
             (is (= (get-in db' [:db :editors :flux :width]) 8))
             (is (= (get-in db' [:db :editors :data :width]) 16)))))
    
(testing "Test updating the width of transformation editor"
  (let [long-transformation-content "as-lines\n
                                     |decode-formeta\n
                                     |fix\n
                                     |encode-xml (rootTag= \"collection25481354555465645656454\")"
        db' (-> empty-db
                (events/update-width [:update-width :flux long-transformation-content]))]
    (and (is (= (get-in db' [:db :editors :transformation :width]) 16))
         (is (= (get-in db' [:db :editors :flux :width]) 16))
         (is (= (get-in db' [:db :editors :data :width]) 16)))))))
