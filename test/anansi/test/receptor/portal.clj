(ns anansi.test.receptor.portal
  (:use [anansi.receptor.portal] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest portal
  (set! *print-level* 4)
  (let [r (receptor test-receptor nil "fish")
        p1 (receptor portal r)
        p2 (receptor portal r "x")
        ]
    (testing "default target"
      (is (= (contents p1 :target) r)))
    (testing "custom target"
      (is (= (contents p2 :target) "x")))
    (testing "enter signal"
      (let [o (self->enter p1 "zippy" {:name "Eric H-B" :image "http://gravatar.com/userimage/x.jpg" :phone "123/456-7890"})]
        (is (= (get-receptor r (address-of o)) o)))
      )))
