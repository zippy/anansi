(ns anansi.test.commons-room.occupant
  (:use [anansi.commons-room.occupant] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest occupant
  (let [o (receptor :occupant nil "zippy" {:name "e"})]
    (testing "contents"
      (is (= "zippy" (contents o :unique-name)))
      (is (= {:name "e"} (contents o :data)))
      )
    (testing "update"
      (s-> self->update o {:name "x"} nil)
      (is (= {:name "x"} (contents o :data)))
      (s-> self->update o "y" :name)
      (is (= {:name "y"} (contents o :data)))
      )
    (testing "state"
      (is (= (:unique-name (state o true))
             "zippy"))
      (is (= (:data (state o true))
             {:name "y"})))
    (testing "restore"
      (is (=  (state o true) (state (restore (state o true) nil) true))))
    ))
