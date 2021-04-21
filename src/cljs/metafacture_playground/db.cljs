(ns metafacture-playground.db)

(def sample-fields
  {:data {:content "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}\n 2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}"
          :collapsed? false}
   :flux {:content "as-lines\n|decode-formeta\n|fix\n|stream-to-xml(rootTag=\"collection\")"
          :collapsed? false}
   :fix  {:content "map(_id, id)\nmap(a,title)\nmap(b.n,author)\n/*map(_else)*/\n"
          :collapsed? false}})

(def default-db
  {:input-fields {:data {:content nil
                         :collapsed? false}
                  :flux {:content nil
                         :collapsed? false}
                  :fix  {:content nil
                         :collapsed? false}}
   :result {:loading? false
            :collapsed? false
            :content nil
            :links {:api-call nil}}})
