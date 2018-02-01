(defproject clusters "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [reagent "0.7.0"]
                 ;; [reagent "0.8.0-alpha2"]
                 [org.clojure/core.async "0.4.474"]
                 [com.taoensso/timbre "4.8.0"]
                 [re-com "2.1.0"]
                 [com.rpl/specter "1.1.0"]
                 [org.clojure/clojurescript "1.9.946"]]

  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test/clj" "test/cljc"]
  
  :plugins [[lein-cljsbuild "1.1.7"  :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.13"]]

  :figwheel {:css-dirs ["resources/public/css"]}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" :target-path]
  
  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src/cljs" "src/cljc" "test/cljs" "test/cljc"]
     :figwheel {:on-jsload "clusters.core/on-js-reload"
                :open-urls ["http://localhost:3449/index.html"]}
     :compiler {:main clusters.core
                :asset-path "js/compiled/out"
                :output-to "resources/public/js/compiled/clusters.js"
                :output-dir "resources/public/js/compiled/out"
                :source-map-timestamp true
                :preloads [devtools.preload]}}]}

 :profiles {:dev
            {:source-paths ["test/cljc"]
             :dependencies [[binaryage/devtools "0.9.4"]
                             [figwheel-sidecar "0.5.13"]
                             [com.cemerick/piggieback "0.2.2"]
                             [org.clojure/tools.nrepl "0.2.13"]]

              :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}})
