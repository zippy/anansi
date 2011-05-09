(ns anansi.test.receptor.list-scape
  (:use [anansi.receptor.list-scape] :reload)
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [clojure.test]))

(deftest list-scape
  (let [s (receptor list-scape nil)]
    (testing "signal"
      (is (= 0 (address->push s 19)))
      (is (= 19 (key->resolve s 0))))
    ))
