(ns anansi.test.receptor.occupant
  (:use [anansi.receptor.occupant] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest occupant
  (let [o (receptor occupant nil "zippy" {:name "e"})]
    (testing "contents"
      (is (= "zippy" (contents o :unique-name)))
      (is (= {:name "e"} (contents o :data)))
      )))
