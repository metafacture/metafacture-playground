(ns metafacture-playground.db)

; TODO: we should extract the samples here and in process_test.clj to files
(def sample-data {:data "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}\n2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}"
                  :flux-with-fix "as-lines\n|decode-formeta\n|fix\n|encode-xml(rootTag=\"collection\")"
                  :flux-with-morph "as-lines\n|decode-formeta\n|morph\n|encode-xml(rootTag=\"collection\")"
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

(def sample-fields
  {:data {:content (:data sample-data)
          :collapsed? false}
   :flux {:content (:flux-with-fix sample-data)
          :collapsed? false}
   :fix  {:content (:fix sample-data)}
   :morph {:content (:morph sample-data)}
   :switch {:collapsed? false
            :active :fix}})

(def default-db
  {:input-fields {:data {:content nil
                         :collapsed? false
                         :width nil}
                  :flux {:content nil
                         :collapsed? false
                         :width nil}
                  :fix {:content nil}
                  :morph {:content nil}
                  :switch {:collapsed? false
                           :active :fix
                           :width nil}}
   :links {:api-call nil
           :workflow nil}
   :result {:loading? false
            :collapsed? false
            :content nil}
   :message {:content nil
             :type nil}
   :ui {:height nil}})

(defn- parseBoolean [val]
  (if (= val "true")
    true
    false))

(def db-parse-fns
  {:input-fields {:data {:content str
                         :collapsed? parseBoolean
                         :width int}
                  :flux {:content str
                         :collapsed? parseBoolean
                         :width int}
                  :fix {:content str}
                  :morph {:content str}
                  :switch {:collapsed? parseBoolean
                           :active keyword
                           :width int}}
   :links {:api-call str
           :workflow str}
   :result {:loading? parseBoolean
            :collapsed? parseBoolean
            :content str}
   :message {:content str
             :type keyword}
   :ui {:height parseBoolean}})
