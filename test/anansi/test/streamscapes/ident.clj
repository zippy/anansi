(ns anansi.test.streamscapes.ident
  (:use [anansi.streamscapes.ident] :reload)
  (:use [anansi.ceptr])
  (:use [anansi.streamscapes.streamscapes])
  (:use [clojure.test]))

(deftest ident
  (let [i (receptor :ident nil {:name "joe"})]
    (testing "contents"
      (is (= "joe" (contents i :name)))
      )
    (testing "restore"
      (is (=  (state i true) (state (restore (state i true) nil) true))))
    ))
