(ns anansi.test.receptor.occupant
  (:use [anansi.receptor.occupant] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest occupant
  (let [o (receptor occupant nil "zippy" {:name "e"})]
    (testing "contents"
      (is (= "zippy" (contents o :unique-name)))
      (is (= {:name "e"} (contents o :data)))
      )
    (testing "state"
      (is (= (:unique-name (state o true))
             "zippy"))
      (is (= (:data (state o true))
             {:name "e"})))
    (testing "restore"
      (is (=  (state o true) (state (restore (state o true) nil) true))))
    ))
