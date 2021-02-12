(defproject metafacture-playground "0.1.0-SNAPSHOT"
  :description "Web application to play around with workflows using Metafacture languages Fix and Flux"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library
                               org.clojure/google-closure-library-third-party]]
                 [thheller/shadow-cljs "2.11.7"]
                 [reagent "0.10.0"]
                 [re-frame "1.1.2"]
                 [compojure "1.6.2"]
                 [yogthos/config "1.1.7"]
                 [ring "1.8.2"]
                 [org.metafacture/metafacture-commons "feature-oersi-SNAPSHOT"]
                 [org.metafacture/metafacture-formeta "feature-oersi-SNAPSHOT"]
                 [org.metafacture/metafacture-mangling "feature-oersi-SNAPSHOT"]
                 [org.metafacture/metafacture-runner "feature-oersi-SNAPSHOT"]
                 [org.metafacture/metafacture-xml "feature-oersi-SNAPSHOT"]
                 [org.eclipse.xtext/xtext-dev-bom "2.17.0" :extension "pom"]
                 [org.metafacture.fix/org.metafacture.fix "1.0.0-SNAPSHOT" :exclusions [[org.eclipse.xtext/xtext-dev-bom]]]]

  :plugins [[lein-shadow "0.3.1"]

            [lein-shell "0.5.0"]]

  :min-lein-version "2.9.0"

  :source-paths ["src/clj" "src/cljs"]

  :test-paths   ["test/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]


  :shadow-cljs {:nrepl {:port 8777}

                :builds {:app {:target :browser
                               :output-dir "resources/public/js/compiled"
                               :asset-path "/js/compiled"
                               :modules {:app {:init-fn metafacture-playground.core/init
                                               :preloads [devtools.preload]}}

                               :devtools {:http-root "resources/public"
                                          :http-port 8280
                                          :http-handler metafacture-playground.handler/dev-handler}}
                         :browser-test
                         {:target :browser-test
                          :ns-regexp "-test$"
                          :runner-ns shadow.test.browser
                          :test-dir "target/browser-test"
                          :devtools {:http-root "target/browser-test"
                                     :http-port 8290}}

                         :karma-test
                         {:target :karma
                          :ns-regexp "-test$"
                          :output-to "target/karma-test.js"}}}

  :shell {:commands {"karma" {:windows         ["cmd" "/c" "karma"]
                              :default-command "karma"}
                     "open"  {:windows         ["cmd" "/c" "start"]
                              :macosx          "open"
                              :linux           "xdg-open"}}}

  :aliases {"dev"          ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein watch instead.\""]
                            ["watch"]]
            "watch"        ["with-profile" "dev" "do"
                            ["shadow" "watch" "app" "browser-test" "karma-test"]]

            "prod"         ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein release instead.\""]
                            ["release"]]

            "release"      ["with-profile" "prod" "do"
                            ["shadow" "release" "app"]]

            "build-report" ["with-profile" "prod" "do"
                            ["shadow" "run" "shadow.cljs.build-report" "app" "target/build-report.html"]
                            ["shell" "open" "target/build-report.html"]]

            "karma"        ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein ci instead.\""]
                            ["ci"]]
            "ci"           ["with-profile" "prod" "do"
                            ["shadow" "compile" "karma-test"]
                            ["shell" "karma" "start" "--single-run" "--reporters" "junit,dots"]]}

  :main metafacture-playground.server

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "1.0.2"]]
    :source-paths ["dev"]}

   :prod {}

   :uberjar {:source-paths ["env/prod/clj"]
             :omit-source  true
             :main         metafacture-playground.server
             :aot          [metafacture-playground.server]
             :uberjar-name "metafacture-playground.jar"
             :prep-tasks   ["compile" ["release"]]}}

  :prep-tasks [])
