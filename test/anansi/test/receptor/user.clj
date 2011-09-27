(ns anansi.test.receptor.user
  (:use [anansi.receptor.user] :reload)
  (:use [anansi.ceptr])
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest user
  (let [u (make-receptor user-def nil "zippy")]
    (testing "contents"
      (is (= (contents u :name) "zippy"))
;      (is (= (contents u :stream) :the-stream))
      )
    (testing "disconnect signal"
      (s-> self->disconnect u)
      (is (= (contents u :stream) nil)))
    (testing "connect signal"
      (s-> self->connect u :another-stream)
      (is (= (contents u :stream) :another-stream)))
    (fact (receptor-state u true) => (contains {:name "zippy" :fingerprint :anansi.receptor.user.user}))
    (testing "state"
      (is (= (:name (receptor-state u true))
             "zippy")))
    (testing "restore"
      (is (=  (receptor-state u true) (receptor-state (receptor-restore (receptor-state u true) nil) true))))

    ))
