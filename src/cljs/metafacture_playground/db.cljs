(ns metafacture-playground.db)

(def sample-fields
  {:data "1{a: Faust, b {n: Goethe, v: JW}, c: Weimar}\n 2{a: Räuber, b {n: Schiller, v: F}, c: Weimar}"
   :flux "as-lines|decode-formeta|fix|stream-to-xml(rootTag=\"collection\")"
   :fix  "map(_id, id)\nmap(a,title)\nmap(b.n,author)\n/*map(_else)*/\n"})

(def default-db
  {:input-fields {:data ""
                  :flux ""
                  :fix  ""}
   :result {:loading? false
            :content "No result"}})
