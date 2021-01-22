(ns rewrite-clj.node.coercer-test
  (:require [clojure.test :refer [deftest testing is are]]
            [rewrite-clj.node :as node :refer [coerce]]
            [rewrite-clj.node.protocols :as protocols]
            [rewrite-clj.parser :as p]))

(deftest t-sexpr->node->sexpr-roundtrip
  (are [?sexpr expected-tag expected-type]
       (let [n (coerce ?sexpr)]
         (is (node/node? n))
         (is (= expected-tag (node/tag n)))
         (is (= expected-type (protocols/node-type n)))
         (is (string? (node/string n)))
         (is (= ?sexpr (node/sexpr n)))
         (is (not (meta n)))
         (is (= (type ?sexpr) (type (node/sexpr n)))))

    ;; TODO: we have an integer-node, do we use it?
    ;; numbers
    3                      :token      :token
    3N                     :token      :token
    3.14                   :token      :token
    3.14M                  :token      :token
    3e14                   :token      :token

    ;; ratios are not valid in cljs
    #?@(:clj  [3/4         :token      :token])

    ;; symbol/keyword/string/...
    'symbol                :token      :symbol
    'namespace/symbol      :token      :symbol
    :keyword               :token      :keyword
    :1.5.1                 :token      :keyword
    ::keyword              :token      :keyword
    ::1.5.1                :token      :keyword
    :namespace/keyword     :token      :keyword
    ;; TODO: we have a string node, do we use it?
    ""                     :token      :token
    "hello, over there!"   :token      :token
    "multi\nline"          :token      :token
    ;; whitespace is coerced to string token nodes, not whitespace/newline nodes
    " "                    :token      :token
    "\n"                   :token      :token

    ;; seqs
    []                     :vector     :seq
    [1 2 3]                :vector     :seq
    ()                     :list       :seq
    '()                    :list       :seq
    (list 1 2 3)           :list       :seq
    #{}                    :set        :seq
    #{1 2 3}               :set        :seq

    ;; date
    #inst "2014-11-26T00:05:23" :token :token))

(deftest t-sexpr->node->sexpr-roundtrip-for-regex
  (let [sexpr #"abc"
        n (coerce sexpr)]
    (is (node/node? n))
    (is (string? (node/string n)))
    (is (= (str sexpr) (str (node/sexpr n))))
    (is (= (type sexpr) (type (node/sexpr n))))))

(deftest t-vars
  (let [n (coerce #'identity)]
    (is (node/node? n))
    (is (= '(var #?(:clj clojure.core/identity :cljs cljs.core/identity)) (node/sexpr n)))))

(deftest t-nil
  (let [n (coerce nil)]
    (is (node/node? n))
    (is (= nil (node/sexpr n)))
    (is (= n (p/parse-string "nil")))))

(defrecord Foo-Bar [a])

(deftest t-records
  (let [v (Foo-Bar. 0)
        n (coerce v)]
    (is (node/node? n))
    (is (= :reader-macro (node/tag n)))
    (is (= (pr-str v) (node/string n)))))

(deftest t-nodes-coerce-to-themselves
  (testing "parsed nodes"
    ;; lean on the parser to create node structures
    (are [?s ?tag ?type]
         (let [n (p/parse-string ?s)]
           (is (= n (node/coerce n)))
           (is (= ?tag (node/tag n)))
           (is (= ?type (protocols/node-type n))))
      ";; comment"      :comment        :comment
      "#(+ 1 %)"        :fn             :fn
      ":my-kw"          :token          :keyword
      "^:m1 [1 2 3]"    :meta           :meta
      "'a"              :quote          :quote
      "#'var"           :var            :reader
      "#=eval"          :eval           :reader
      "@deref"          :deref          :deref
      "#mymacro 44"     :reader-macro   :reader-macro
      "#\"regex\""      :regex          :regex
      "[1 2 3]"         :vector         :seq
      "42"              :token          :token
      "sym"             :token          :symbol
      "#_ 99"           :uneval         :uneval
      " "               :whitespace     :whitespace
      ","               :comma          :comma
      "\n"              :newline        :newline))
  (testing "parsed forms nodes"
    (let [n (p/parse-string-all "(def a 1)")]
      (is (= n (node/coerce n)))
      (is (= :forms (node/tag n)))))
  (testing "nodes that are not parsed, but can be created manually"
    ;; TODO: there is also indent nodes, but I think it is unused
    (let [n (node/integer-node 10)]
      (is (= n (node/coerce n))))
    (let [n (node/string-node "my-string")]
      (is (= n (node/coerce n))))))