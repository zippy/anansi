(ns anansi.test.ceptr
  (:use [anansi.ceptr] :reload)
  (:use [anansi.receptor.scape])
  (:use [midje.sweet])
  (:use [clojure.test]))

    (set! *print-level* 6)
(defmethod manifest :test-receptor [_r & args]
           ;;{:x (apply str  "the receptor contents: " args)}
           (make-scapes _r {:x (apply str  "the receptor contents: " args)} {:name :s1 :relationship {:key :x :address :y}} :s2)
           )
(defmethod animate :test-receptor [_r]
           (dosync (_set-content _r :animated true)
                   _r))

(comment fact
  (merge-forms '(fish 1 2 3)) => (throws RuntimeException "unknown receptor form 'fish'"))

(fact
  ('attributes (merge-forms '(attributes :a :b :c))) => (contains {:args '(:a :b :c)}))

(facts "about validating receptor forms"
  (validate-forms (merge-forms '(attributes))) => (throws RuntimeException "attributes receptor form requires a list of attributes")
  (validate-forms (merge-forms '(attributes 1 2 3))) => nil)

(def r-def (receptor-def "test-receptor"
              (scapes {:name :s1 :relationship {:key :x :address :y}} :s2)
              (attributes :x)
              (animate [_r reanimate] (dosync (_set-content _r :animated true)
                                    _r))))

(def rsub-def (receptor-def "test-sub-receptor"))

(facts "about receptor definition"
  @*definitions* => (contains {:anansi.test.ceptr.test-receptor r-def})
  (keys r-def) => (just [:fingerprint :attributes :scapes :manifest :animate :state :restore])
  (:fingerprint r-def) => :anansi.test.ceptr.test-receptor
  (:scapes r-def) => #{{:name :s1 :relationship {:key :x :address :y}} :s2}
  (:attributes r-def) => #{:x}
  (:animate r-def) => fn?)


                                        ;(def r (receptor :test-receptor nil "fish"))
(def r (make-receptor r-def nil {:attributes {:x "fish"}}))
(fact (rdef r :attributes) => #{:x})

(signal self test-signal [_r _f param]
        (str "from " _f " with param " param ))

(fact (self->test-signal r nil 1)=> "from  with param 1")
(fact (receptor-state r false) => (contains {:x "fish" :scapes {:s1-scape {:values {}, :relationship {:key :x, :address :y}}, :s2-scape {:values {}, :relationship {:key nil, :address nil}}}}))

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
    (is (= (contents r :x) "fish"))
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
  
  (--> key->set r (get-scape r :s1) :test-key :test-val)
  (facts "about getting state information from a receptor"
    (receptor-state r false) => (contains {:x "the receptor contents: dog"
                                           :scapes {:s1-scape {:values  {:test-key :test-val} :relationship {:key :x :address :y}}, :s2-scape {:values {} :relationship {:key nil :address nil}}}
                                           :address 1
                                           :changes 4
                                           :fingerprint :anansi.test.ceptr.test-receptor
                                           :receptors {:last-address 3}})
    (receptor-state r true) => (contains {:x "the receptor contents: dog"
                                          :scapes-scape-addr 1
                                          :address 1
                                          :changes 4
                                          :fingerprint :anansi.test.ceptr.test-receptor
                                          :receptors {:last-address 3, 3 {:relationship {:key nil, :address nil}, :map {}, :fingerprint :anansi.receptor.scape.scape, :address 3, :changes 0}, 2 {:relationship {:key :x, :address :y}, :map {:test-key :test-val}, :fingerprint :anansi.receptor.scape.scape, :address 2, :changes 1}, 1 {:relationship {:key :scape-name, :address :address}, :map {:s1-scape 2, :s2-scape 3}, :fingerprint :anansi.receptor.scape.scape, :address 1, :changes 2}}})
    )
  (facts "about restoring receptors from state data"
    (let [restored (receptor-restore (receptor-state r true) nil)]
      (receptor-state r true) => (receptor-state restored true)
      (receptor-state (get-scape r :scapes) true) => (receptor-state (get-scape restored :scapes) true)
      (receptor-state (get-scape r :s1) true) => (receptor-state (get-scape restored :s1) true))
    )
  (testing "serialization"
    (let [s (serialize-receptors *receptors*)
          u (unserialize-receptors s)]
      (is (= s (serialize-receptors u))))
    )

(def r2 (receptor-def "r2" (attributes :data :_password)))
(facts "about receptors with private attributes"
  (let [r (make-receptor r2 nil {:attributes {:data "fish" :_password "secret"}})]
    (receptor-state r true) => (contains {:data "fish" :_password "secret"})
    (contains? (receptor-state r false) :_password) => false
    )
  )

(facts "about querys on public access to receptor contents"
  (let [p (make-receptor r-def nil {:attributes {:x "parent"}})
        x (address-of (make-receptor rsub-def p {}))
        z (address-of (make-receptor r-def p {:attributes {:x "zippy"}}))
        s (address-of (make-receptor r-def p {:attributes {:x "sam"}}))
        j (address-of (make-receptor r-def p {:attributes {:x "jane"}}))
        ]
    (--> key->set p (get-scape p :s1) "zippy" z)
    (--> key->set p (get-scape p :s1) "sam" s)
    (--> key->set p (get-scape p :s1) "jane" j)
    (--> key->set p (get-scape p :s2) z "zippy")
    (--> key->set p (get-scape p :s2) s "sam")
    (--> key->set p (get-scape p :s2) j "jane")
    
    (set (keys (:receptors (receptor-state p {:scape-query {:scape :s1 :query [">" "s"]}})))) => #{s z}
    (set (keys (:receptors (receptor-state p {:scape-query {:scape :s2 :query [">" "s"] :flip true}})))) => #{s z}
    (set (keys (:receptors (receptor-state p {:scape-query {:scape :s1 :query ["<" "s"]}})))) => #{j}
    (set (keys (:receptors (receptor-state p {:scape-query {:scape :s2 :query ["<" "s"] :flip true}})))) => #{j}
    (set (keys (:receptors (receptor-state p {:scape-query {:scape :s1 :query ["=" "sam"]}})))) => #{s}
    (set (keys (:receptors (receptor-state p {:scape-query {:scape :s2 :query ["=" "sam"] :flip true}})))) => #{s}
    (:receptor-order (receptor-state p {:scape-order {:scape :s1}})) => [j s z]
    (:receptor-order (receptor-state p {:scape-order {:scape :s1 :descending true}})) => [z s j]
    (:receptor-order (receptor-state p {:scape-order {:flip true :scape :s2 :descending true}})) => [z s j]
    (let [state (receptor-state p {:receptor 0 :scape-order {:scape :s1 :limit 2}})]
      (:receptor-total state) => 3
      (:receptor-order state) => [j s]
      (set (keys (:receptors state))) => #{j s x}
      )
    (let [state (receptor-state p {:scape-order {:scape :s1 :offset 1}})]
      (:receptor-order state) => [s z]
      (set (keys (:receptors state))) => #{s z x}
      )
    (let [state (receptor-state p {:scape-order {:scape :s1 :limit 1 :offset 1}})]
      (:receptor-order state) => [s]
      (set (keys (:receptors state))) => #{s x}
      )
    (let [state (receptor-state p {:scape-order {:scape :s1 :limit 1 :offset 1 :scape-receptors-only true}})]
      (:receptor-order state) => [s]
      (set (keys (:receptors state))) => #{s}
      )
))
