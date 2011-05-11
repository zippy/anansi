(ns anansi.test.ceptr
  (:use [anansi.ceptr] :reload)
  (:use [clojure.test]))

(defmethod manifest :test-receptor [_r & args]
           {:x (apply str  "the receptor contents: " args)})

(def r (receptor test-receptor nil "fish"))
(signal self test-signal [_r param]
        param )
(deftest signaling
  (testing "creating a signal"
    (is (= (self->test-signal r 1) 1)))
  )
(deftest receptors
  (testing "contents"
    (is (= (contents r :x) "the receptor contents: fish")))
  (testing "instantiate receptor"
    (is (= r (get-receptor nil (address-of r))))
    )
  (testing "addresses"
    (is (= (address-of r) 1))
    (is (= r (get-receptor (parent-of r) (address-of r))))))
