(defproject watermark "0.1.0-SNAPSHOT"
  :description "Apply a watermark to the bottom right of images"
  :url "http://example.com/FIXME"
  :main ^:skip-aot watermark.core
  :repl {:plugins [[cider/cider-nrepl "0.13.0"]]}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]]
  :plugins [[lein-bikeshed "0.3.0"]
            [jonase/eastwood "0.2.3"]
            [lein-cljfmt "0.5.3"]
            [lein-ring "0.9.7"]]
  :ring {:handler webapp.core/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
