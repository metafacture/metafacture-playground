(ns metafacture-playground.process-test
  (:require [clojure.test :refer :all]
            [metafacture-playground.process :refer [process]]))

(def sample-data {:data "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}\n 2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}"
                  :flux "as-lines|decode-formeta|fix|stream-to-xml(rootTag=\"collection\")"
                  :fix  "map(_id, id)\nmap(a,title)\nmap(b.n,author)\n/*map(_else)*/\n"
                  :result (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                               "\n<collection>"
                               "\n"
                               "\n	<record>"
                               "\n		<id>1</id>"
                               "\n		<title>Faust</title>"
                               "\n		<author>Goethe</author>"
                               "\n	</record>"
                               "\n"
                               "\n	<record>"
                               "\n		<id>2</id>"
                               "\n		<title>Räuber</title>"
                               "\n		<author>Schiller</author>"
                               "\n	</record>"
                               "\n"
                               "\n</collection>\n")})

(deftest processing-test
  (testing "Process sample data correctly."
    (let [result (process (:data sample-data) (:flux sample-data) (:fix sample-data))]
      (is (= result (:result sample-data))))))

