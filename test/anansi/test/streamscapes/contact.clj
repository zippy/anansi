(ns anansi.test.streamscapes.contact
  (:use [anansi.streamscapes.contact] :reload)
  (:use [anansi.ceptr])
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest contact
  (let [i (make-receptor contact-def nil {:attributes {:name "joe"}})]
    (fact (receptor-state i false) => (contains {:name "joe" :fingerprint :anansi.streamscapes.contact.contact}))
    (testing "contents"
      (is (= "joe" (contents i :name)))
      )
    (testing "restore"
      (is (=  (receptor-state i true) (receptor-state (receptor-restore (receptor-state i true) nil) true))))
    ))
