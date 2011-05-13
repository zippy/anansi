(ns anansi.test.ceptr
  (:use [anansi.ceptr] :reload)
  (:use [anansi.receptor.scape])
  (:use [clojure.test]))

    (set! *print-level* 6)
(defmethod manifest :test-receptor [_r & args]
           ;;{:x (apply str  "the receptor contents: " args)}
           (make-scapes _r {:x (apply str  "the receptor contents: " args)} :s1 :s2)
           )

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
  (set! *print-level* 8)
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
  (testing "state"
    (receptor test-receptor r "cow")
    (--> key->set r (contents r :s1-scape) :test-key :test-val)
    (is (= {:scapes {:s1-scape {:test-key :test-val}, :s2-scape {}}, :type :test-receptor, :address 1, :receptors {4 {:scapes {:s1-scape {}, :s2-scape {}}, :type :test-receptor, :address 4, :receptors {}}}}
           (state r))))
  )
