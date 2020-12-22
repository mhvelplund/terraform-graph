(defproject terraform-graph "1.0.0"
  :description "Generate an XGML dependency graph from a Terraform state file"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "1.0.0"]
                 [org.clojure/data.xml "0.0.8"]]
  ;; :main ^:skip-aot terraform-graph.core
  :main terraform-graph.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
