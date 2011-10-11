(ns anansi.test.streamscapes.ident
  (:use [anansi.streamscapes.ident] :reload)
  (:use [anansi.ceptr])
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest ident
  (let [i (make-receptor ident-def nil {:attributes {:name "joe"}})]
    (fact (receptor-state i false) => (contains {:name "joe" :fingerprint :anansi.streamscapes.ident.ident}))
    (testing "contents"
      (is (= "joe" (contents i :name)))
      )
    (testing "restore"
      (is (=  (receptor-state i true) (receptor-state (receptor-restore (receptor-state i true) nil) true))))
    ))
