(ns metafacture-playground.db)

(def sample-fields
  {:data {:content "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}\n2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}"
          :collapsed? false}
   :flux {:content "as-lines\n|decode-formeta\n|fix\n|stream-to-xml(rootTag=\"collection\")"
          :collapsed? false}
   :fix  {:content "map(_id, id)\nmap(a,title)\nmap(b.n,author)\n/*map(_else)*/"}
   :morph {:content (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                         "<metamorph xmlns=\"http://www.culturegraph.org/metamorph\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                         "\tversion=\"1\">\n"
                         "\t<rules>\n"
                         "\t\t<data source=\"_id\" name=\"id\"/>\n"
                         "\t\t<data source=\"a\" name=\"title\"/>\n"
                         "\t\t<concat delimiter=\", \" name=\"author\">\n"
                         "\t\t\t<data source=\"b.n\" />\n"
                         "\t\t\t<data source=\"b.v\" />\n"
                         "\t\t</concat>\n"
                         "\t</rules>\n"
                         "</metamorph>\n")}
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
