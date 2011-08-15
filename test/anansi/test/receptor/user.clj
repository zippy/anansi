(ns anansi.test.receptor.user
  (:use [anansi.receptor.user] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest user
  (let [u (receptor :user nil "zippy" :the-stream)]
    (testing "contents"
      (is (= (contents u :name) "zippy"))
      (is (= (contents u :stream) :the-stream)))
    (testing "disconnect signal"
      (s-> self->disconnect u)
      (is (= (contents u :stream) nil)))
    (testing "connect signal"
      (s-> self->connect u :another-stream)
      (is (= (contents u :stream) :another-stream)))
    (testing "state"
      (is (= (:name (state u true))
             "zippy")))
    (testing "restore"
      (is (=  (state u true) (state (restore (state u true) nil) true))))

    ))
