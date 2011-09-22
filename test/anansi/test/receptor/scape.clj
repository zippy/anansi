(ns anansi.test.receptor.scape
  (:use [anansi.receptor.scape] :reload)
  (:use [anansi.ceptr])
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest scape
  (let [s (receptor :scape nil :name :address)]
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
    (testing "restore"
      (is (=  (state s true) (state (restore (state s true) nil) true))))
    
    ))

(let [s (receptor :scape nil :a :b)]
  (s-> key->set s :a 1)
  (facts "Serialization"
    (:map (state s false)) => (just {:a 1})
    (state s false) => (contains {:type :scape, :relationship {:key :a :address :b}})
    (state s true) => (contains {:type :scape, :relationship {:key :a :address :b}})))

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

(facts "about creating scapes during manifestion"
  (let [r (receptor :test-receptor nil)
        m (make-scapes r {:x :y} {:name :a :relationship {:key :r1 :address :r2}} :b)
        ss (state (:scapes-scape m) true)
        ]
    (keys m) => (just #{:scapes-scape :a-scape :b-scape :x})
    ss => (contains {:relationship {:key :scape-name, :address :address}})
    (keys (:map ss)) => (just #{:a-scape :b-scape})
    (state (get-receptor r (s-> key->resolve (:scapes-scape m) :a-scape)) true) => (contains {:relationship {:key :r1, :address :r2}, :map {}})
))

(facts "relationship description"
  (let [r (receptor :scape nil :address :name)]
    (scape-relationship r :key) => :address
    (scape-relationship r :address) => :name
    ))
