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
  (set! *print-level* 10)
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
  (testing "get-scape"
    (is (= (get-scape r :s1-scape) (get-receptor r (--> key->resolve r (get-receptor r 1) :s1-scape )))))
  (testing "state"
    (receptor test-receptor r "cow")
    
    (--> key->set r (contents r :s1-scape) :test-key :test-val)
    (is (= (state r false)
           {:scapes {:s1-scape {:test-key :test-val}, :s2-scape {}}, :receptors {:last-address 4, 4 {:scapes {:s1-scape {}, :s2-scape {}}, :receptors {:last-address 3}, :type :test-receptor, :address 4, :changes 4}}, :type :test-receptor, :address 1, :changes 4}))
    (is (= (state r true)
            {:scapes-scape-addr 1, :receptors {:last-address 4, 4 {:scapes-scape-addr 1, :receptors {:last-address 3, 3 {:map {}, :type :scape, :address 3, :changes 0}, 2 {:map {}, :type :scape, :address 2, :changes 0}, 1 {:map {:s1-scape 2, :s2-scape 3}, :type :scape, :address 1, :changes 2}}, :type :test-receptor, :address 4, :changes 4}, 3 {:map {}, :type :scape, :address 3, :changes 0}, 2 {:map {:test-key :test-val}, :type :scape, :address 2, :changes 1}, 1 {:map {:s1-scape 2, :s2-scape 3}, :type :scape, :address 1, :changes 2}}, :type :test-receptor, :address 1, :changes 4})))
  
  (testing "restore"
    (let [restored (restore (state r true) nil)]
      (is (= (state r true) (state restored true)))
      (is (= (state (get-scape r :scapes-scape) true) (state (get-scape restored :scapes-scape) true)))
      (is (= (state (get-scape r :s1-scape) true) (state (get-scape restored :s1-scape) true))))
    )
  (testing "serialization"
    (let [s (serialize-receptors *receptors*)
          u (unserialize-receptors s)]
      (is (= s (serialize-receptors u))))
    )
  )
