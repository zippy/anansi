(ns anansi.test.receptor.portal
  (:use [anansi.receptor.portal] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest portal
  (set! *print-level* 6)
  (let [r (receptor test-receptor nil "fish")
        p1 (receptor portal r)
        p2 (receptor portal r (address-of p1))
        ]
    (testing "default target"
      (is (= (contents p1 :target) 0)))
    (testing "custom target"
      (is (= (contents p2 :target) (address-of p1))))
    (testing "enter signal"
      (let [o (s-> self->enter p1 "zippy" {:name "Eric H-B" :image "http://gravatar.com/userimage/x.jpg" :phone "123/456-7890"})]
        (is (= (get-receptor r (address-of o)) o))))
    (testing "state"
      (is (= (:target (state p1 true))
             0)))
    (testing "restore"
      (is (=  (state p1 true) (state (restore (state p1 true) nil) true))))
    ))
