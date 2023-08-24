(ns metafacture-playground.process-test
  (:require [clojure.test :refer :all]
            [metafacture-playground.process :refer [process]]))

; TODO: we should extract the samples here and in db.cljs to files
(def sample-data {:data "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}\n2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}"
                  :flux-with-fix "infile|\nopen-file\n|as-lines\n|decode-formeta\n|fix(transformationfile)\n|encode-xml(rootTag=\"collection\")\n|print\n;"
                  :flux-with-morph "infile|\nopen-file\n|as-lines\n|decode-formeta\n|morph(transformationfile)\n|encode-xml(rootTag=\"collection\")\n|print\n;"
                  :fix  "move_field(_id, id)\nmove_field(a, title)\npaste(author, b.v, b.n, '~aus', c)\nretain(id, title, author)"
                  :morph  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                         "<metamorph xmlns=\"http://www.culturegraph.org/metamorph\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                         "\tversion=\"1\">\n"
                         "\t<rules>\n"
                         "\t\t<data source=\"_id\" name=\"id\"/>\n"
                         "\t\t<data source=\"a\" name=\"title\"/>\n"
                         "\t\t<combine value=\"${first} ${last} aus ${place}\" name=\"author\">\n"
                         "\t\t\t<data source=\"b.v\" name=\"first\" />\n"
                         "\t\t\t<data source=\"b.n\" name=\"last\" />\n"
                         "\t\t\t<data source=\"c\" name=\"place\" />\n"
                         "\t\t</combine>\n"
                         "\t</rules>\n"
                         "</metamorph>\n")})

(def test-data (merge sample-data 
  {:result (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    "\n<collection>"
    "\n"
    "\n	<record>"
    "\n		<id>1</id>"
    "\n		<title>Faust</title>"
    "\n		<author>JW Goethe aus Weimar</author>"
    "\n	</record>"
    "\n"
    "\n	<record>"
    "\n		<id>2</id>"
    "\n		<title>Räuber</title>"
    "\n		<author>F Schiller aus Weimar</author>"
    "\n	</record>"
    "\n"
    "\n</collection>\n")}))

(deftest processing-test
  (testing "Process sample data (with fix) correctly."
    (let [result (process (:flux-with-fix test-data) (:data test-data) (:fix test-data))]
      (is (= result (:result test-data)))))

  (testing "Process sample data (with morph) correctly."
    (let [result (process (:flux-with-morph test-data) (:data test-data) (:morph test-data))]
      (is (= result (:result test-data))))))
