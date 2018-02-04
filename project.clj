(defproject playing-with-threejs "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]]

  :plugins [[lein-figwheel "0.5.14"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel {:websocket-host :js-client-host}
                        :compiler {:main play.core
                                   :asset-path "js/out"
                                   :output-to "resources/public/js/play.js"
                                   :output-dir "resources/public/js/out"
                                   :source-map-timestamp true
                                   :preloads [devtools.preload]}}
                       {:id "min"
                        :source-paths ["src"]
                        :compiler {:output-to "docs/js/play.js"
                                   :main play.core
                                   :optimizations :advanced
                                   :pretty-print false
                                   :externs ["resources/lib/three.ext.js"]}}]}

  :figwheel {:css-dirs ["resources/public/css"]
             :open-file-command "open-in-intellij"
             :server-ip "0.0.0.0"
             :repl false}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.9"]]
                   :clean-targets ^{:protect false} ["resources/public/js" :target-path]}}

  :aliases {"dev" ["do"
                   ["clean"]
                   ["figwheel"]]
            "dist" ["do"
                    ["clean"]
                    ["cljsbuild" "once" "min"]]})
