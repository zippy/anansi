(ns anansi.test.ceptr
  (:use [anansi.ceptr] :reload)
  (:use [clojure.test]))

(defmethod manifest :test-receptor [_r & args]
           {:x (apply str  "the receptor contents: " args)})

(def r (receptor test-receptor nil "fish"))
(signal self test-signal [_r _f param]
        (str "from " _f " with param " param ))
(deftest signaling
  (testing "creating a signal"
    (is (= (self->test-signal r nil 1) "from  with param 1")))
  (testing "sending a signal with destination as address"
    (is (= (--> self->test-signal r (address-of r) :x) "from 1 with param :x")))
  (testing "sending a signal with destination as receptor"
    (is (= (--> self->test-signal r r :x) "from 1 with param :x")))
  (testing "sending a signal to yourself"
    (is (= (s-> self->test-signal r :x) "from 1 with param :x")))
  )
(deftest receptors
  (testing "contents"
    (is (= (contents r :x) "the receptor contents: fish"))
    (set-content r :x "the receptor contents: dog")
    (is (= (contents r :x) "the receptor contents: dog")))
  (testing "instantiate receptor"
    (is (= r (get-receptor nil (address-of r)))))
  (testing "addresses"
    (is (= (address-of r) 1))
    (is (= r (get-receptor (parent-of r) (address-of r)))))
  (testing "destroy receptor"
    (destroy-receptor nil (address-of r))
    (is (= nil (get-receptor nil (address-of r)))))  
    )
