#!/usr/bin/env bb

(ns test-shadow-cljs
  (:require [clojure.java.io :as io]
            [helper.env :as env]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(def compiled-tests "target/shadow-cljs/node-test.js")

(def shadow-cljs-cfg {:source-paths ["src" "test"]
                      :builds {:test {:target :node-test
                                      :output-to compiled-tests
                                      :compiler-options {:warnings
                                                         ;; clj-kondo handles deprecation warnings for us
                                                         {:fn-deprecated false}}}}})

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (status/line :head "testing ClojureScript source with Shadow CLJS, node, optimizations: none")
    (let [shadow-config-file (io/file "shadow-cljs.edn")]
      (.deleteOnExit shadow-config-file)
      (spit shadow-config-file shadow-cljs-cfg)
      (shell/command (if (= :win (env/get-os)) "npx.cmd" "npx")
                     "shadow-cljs" "compile" "test")
      (shell/command "node" compiled-tests)))
  nil)

(main/when-invoked-as-script
 (apply -main *command-line-args*))
