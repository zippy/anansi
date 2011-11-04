(ns anansi.test.receptor.scape
  (:use [anansi.receptor.scape] :reload)
  (:use [anansi.ceptr])
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest scape
  (let [s (make-receptor scape-def nil :name :address)]
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
      (is (=  (receptor-state s true) (receptor-state (receptor-restore (receptor-state s true) nil) true))))
    
    ))

(let [s (make-receptor scape-def nil :a :b)]
  (s-> key->set s :a 1)
  (facts "Serialization"
    (:map (receptor-state s false)) => (just {:a 1})
    (receptor-state s false) => (contains {:fingerprint :anansi.receptor.scape.scape, :relationship {:key :a :address :b}})
    (receptor-state s true) => (contains {:fingerprint :anansi.receptor.scape.scape, :relationship {:key :a :address :b}})))

(def t-def (receptor-def "test-receptor" (scapes :s1)))

(deftest scape-creation-test
  (let [r (make-receptor t-def nil {})]
    (testing "get-scape"
      (is (= (get-scape r :s1) (get-receptor r (--> key->resolve r (get-receptor r 1) :s1-scape ))))
      (is (thrown-with-msg? RuntimeException #":fish scape doesn't exist" (get-scape r :fish)))
      (is (= (get-scape r :squid true) (get-receptor r (--> key->resolve r (get-receptor r 1) :squid-scape))))
      (let [s (get-scape r :animal {:key :name :address :address})]
        (is (= s (get-receptor r (--> key->resolve r (get-receptor r 1) :animal-scape))))
        (is (= (scape-relationship s :key) :name))
        (is (= (scape-relationship s :address) :address)) 
        )
      )
    (testing "add-scape"
      (is (= (add-scape r :boink) (get-receptor r (--> key->resolve r (get-receptor r 1) :boink-scape ))))
      (is (thrown-with-msg? RuntimeException #":boink scape already exists" (add-scape r :boink)))
      (let [s (add-scape r {:name :fish :relationship {:key :fish-name, :address :address}})]
        (is (contains? (:scapes (receptor-state r false)) :fish-scape))
        (is (= (scape-relationship s :key) :fish-name))
        (is (= (scape-relationship s :address) :address)) 
        ))
    (facts "about renaming scapes"
      (let [b (get-scape r :boink)]
        (rename-scape r {:name :boink :new-name :boink}) => (throws RuntimeException ":boink scape already exists")
        (rename-scape r {:name :boink :new-name :zippy})
        (get-scape r :zippy) => b
        )
      )
    (facts "aboout deleting scapes"
      (delete-scape r {:name :asdf}) => (throws RuntimeException ":asdf scape doesn't exist")
      (delete-scape r {:name :zippy})
      (get-scape r :zippy) => (throws RuntimeException ":zippy scape doesn't exist")
      )
    (testing "make-scapes"
      (let [m (make-scapes r {:x :y} :a :b)]
        (is (= #{:x :a-scape :b-scape :scapes-scape} (into #{} (keys m))))
        (is ()))
      )
  
    ))

(facts "about creating scapes during manifestion"
  (let [r (make-receptor t-def nil {})
        m (make-scapes r {:x :y} {:name :a :relationship {:key :r1 :address :r2}} :b)
        ss (receptor-state (:scapes-scape m) true)
        ]
    (keys m) => (just #{:scapes-scape :a-scape :b-scape :x})
    ss => (contains {:relationship {:key :scape-name, :address :address}})
    (keys (:map ss)) => (just #{:a-scape :b-scape})
    (receptor-state (get-receptor r (s-> key->resolve (:scapes-scape m) :a-scape)) true) => (contains {:relationship {:key :r1, :address :r2}, :map {}})
))
  (set! *print-level* 6)

(facts "about scape relationship"
  (let [p (make-receptor t-def nil {})
        r (make-receptor scape-def p :address :name)
        r1 (make-receptor scape-def p :name :frog)
        ]
    (scape-relationship r :key) => :address
    (scape-relationship r :address) => :name
    (find-scapes-by-relationship p :frog) => [r1]
    (s-> key->set r 1 "jose")
    (s-> key->set r 2 "jose")
    (s-> key->set r 3 "bill")
    (s-> key->set r1 "jose" "bull")
    (s-> key->set r1  "peter" "tree")
    (destroy-scape-entries p :name "jose")
    (s-> key->all r) => [3]
    (s-> key->all r1) => ["peter"]
    ))

(facts "about scape querying"
  (let [r (make-receptor scape-def nil :address :value)]
    (doall (map (fn [i x] (s-> key->set r i x)) (range 10) (reverse (range 10))))
    (set (query-scape r (fn ([address value] [(< value 5) address])))) => #{5 6 7 8 9}
    )
  )

(facts "about sorting by scape"
  (let [r (make-receptor scape-def nil :address :value)]
    (doall (map (fn [i x] (s-> key->set r i x)) (map #(nth "zbcdefghij" %) (range 10)) (reverse (range 10))))
    (sort-by-scape r [0 1 2 7 8 9] false) => [8 7 2 1 0 9]
    (sort-by-scape r [0 1 2 7 8 9] false :descending) => [9 0 1 2 7 8]
    )
  )

(facts "about query aspect"
  (let [r (make-receptor scape-def nil :address :value)]
    (s-> key->set r :a 1)
    (s-> key->set r :b 2)
    (s-> query->all r) => [[:a 1] [:b 2]]
    ))
