(ns metafacture-playground.process-test
  (:require [clojure.test :refer :all]
            [metafacture-playground.process :refer [process]]))

(def sample-data {:data "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}\n 2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}"
                  :flux-with-fix "as-lines|decode-formeta|fix|stream-to-xml(rootTag=\"collection\")"
                  :flux-with-morph "as-lines|decode-formeta|morph|stream-to-xml(rootTag=\"collection\")"
                  :fix  "map(_id, id)\nmap(a,title)\nmap(b.n,author)\n/*map(_else)*/\n"
                  :morph (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                              "<metamorph xmlns=\"http://www.culturegraph.org/metamorph\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                              "\tversion=\"1\">\n"
                              "\t<rules>\n"
                              "\t\t<data source=\"_id\" name=\"id\"/>\n"
                              "\t\t<data source=\"a\" name=\"title\"/>\n"
                              "\t\t<concat delimiter=\", \" name=\"author\">\n"
                              "\t\t\t<data source=\"b.n\" />\n"
                              "\t\t</concat>\n"
                              "\t</rules>\n"
                              "</metamorph>\n")
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
  (testing "Process sample data (with fix) correctly."
    (let [result (process (:data sample-data) (:flux-with-fix sample-data) (:fix sample-data) (:morph sample-data))]
      (is (= result (:result sample-data)))))

  (testing "Process sample data (with morph) correctly."
    (let [result (process (:data sample-data) (:flux-with-morph sample-data) (:fix sample-data) (:morph sample-data))]
      (is (= result (:result sample-data))))))  

