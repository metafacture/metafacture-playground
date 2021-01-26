(ns metafacture-playground.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [metafacture-playground.core :as core]))

(deftest fake-test
  (testing "fake description"
    (is (= 1 2))))
