(ns anansi.test.receptor.user
  (:use [anansi.receptor.user] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest user
  (let [u (receptor user nil "zippy" :the-stream)]
    (testing "contents"
      (is (= (contents u :name) "zippy"))
      (is (= (contents u :stream) :the-stream)))
    (testing "disconnect signal"
      (self->disconnect u)
      (is (= (contents u :stream) nil)))
    (testing "connect signal"
      (self->connect u :another-stream)
      (is (= (contents u :stream) :another-stream)))
    ))
