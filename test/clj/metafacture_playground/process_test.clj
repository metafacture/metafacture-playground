(ns metafacture-playground.process-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :refer [blank?]]
            [clojure.tools.logging :as log]
            [metafacture-playground.process :refer [process]]
            [clojure.java.io :as io]
            [lambdaisland.uri :refer [uri query-string->map]]))

(deftest processing-correctly-test
  (let [test-data {:data "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}\n2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}"
                   :flux-with-fix "inputFile|\nopen-file\n|as-lines\n|decode-formeta\n|fix(transformationFile)\n|encode-xml(rootTag=\"collection\")\n|print\n;"
                   :flux-with-morph "inputFile|\nopen-file\n|as-lines\n|decode-formeta\n|morph(transformationFile)\n|encode-xml(rootTag=\"collection\")\n|print\n;"
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
                                "</metamorph>\n")
                   :result (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
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
                                "\n</collection>\n")}]
   (testing "Process sample data (with fix) with correct result."
     (let [result (process (:flux-with-fix test-data) (:data test-data) (:fix test-data))]
       (is (= result (:result test-data)))))

    (testing "Process sample data (with morph) with correct result."
      (let [result (process (:flux-with-morph test-data) (:data test-data) (:morph test-data))]
        (is (= result (:result test-data)))))))

(deftest process-examples-test
  (testing "Process all examples in resources/examples and assert that the result is not blank (nil, empty, or contains only whitespace)."
     (let [examples (->> (io/resource "examples")
                         io/file
                         file-seq
                         (keep #(when (.isFile %)
                                  [(.getPath %) (slurp %)]))
                         (map (fn [[path example]]
                                [path (-> example uri :query query-string->map)])))]
       (doseq [[path example] examples]
         (log/info "Testing example " path)
         (is (not (blank? (process (:flux example) (:data example) (:transformation example)))))))))
