(ns anansi.test.receptor.user
  (:use [anansi.receptor.user] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest user
  (let [o (receptor user nil "zippy" :the-stream)]
    (testing "contents"
      (is (= (contents o :name) "zippy"))
      (is (= (contents o :stream) :the-stream)))
    ))
