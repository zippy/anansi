(ns anansi.test.receptor.scape
  (:use [anansi.receptor.scape] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest scape
  (let [s (receptor :scape nil)]
    (s-> key->set s :a 1)
    (s-> key->set s :b 1)
    (testing "contents"
      (is (= @(contents s :map) {:a 1,:b 1})))
    (testing "setting and resolving"
      (is (= 1 (s-> key->resolve s :a)))
      (is (= [:a :b] (s-> address->resolve s 1)))
      (is (= [:a :b] (into [] (s-> key->all s))))
      (is (= [1] (into [] (s-> address->all s)))))
    (testing "removing"
        (s-> key->delete s :a)
        (is (= nil (s-> key->resolve s :a)))
        (is (= [:b] (s-> address->resolve s 1)))
        (s-> address->delete s 1)
        (is (= [] (s-> address->resolve s 1))))
    (testing "state"
      (s-> key->set s :b 1)
      (is (= (:map (state s true))
             {:b 1})))
    (testing "restore"
      (is (=  (state s true) (state (restore (state s true) nil) true))))
    
    ))

(deftest scape-creation-test
  (let [r (receptor :test-receptor nil)]
    (testing "get-scape"
      (is (= (get-scape r :s1) (get-receptor r (--> key->resolve r (get-receptor r 1) :s1-scape ))))
      (is (thrown-with-msg? RuntimeException #":fish scape doesn't exist" (get-scape r :fish)))
      (is (= (get-scape r :squid true) (get-receptor r (--> key->resolve r (get-receptor r 1) :squid-scape ))))
      )
    (testing "add-scape"
      (is (= (add-scape r :boink) (get-receptor r (--> key->resolve r (get-receptor r 1) :boink-scape ))))
      (is (thrown-with-msg? RuntimeException #":boink scape already exists" (add-scape r :boink)))
      )
    (testing "make-scapes"
      (let [m (make-scapes r {:x :y} :a :b)]
        (is (= #{:x :a-scape :b-scape :scapes-scape} (into #{} (keys m))))
        (is ()))
      )))
