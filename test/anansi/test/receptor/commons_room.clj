(ns anansi.test.receptor.commons-room
  (:use [anansi.receptor.commons-room] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]   
        [anansi.receptor.user])
  (:use [clojure.test]))

(deftest commons-room
  (let [m (receptor user nil "eric" nil)
        u (receptor user nil "zippy" nil)
        u-art (receptor user nil "art" nil)
        r (receptor commons-room nil (address-of m) "password")
        occupants (contents r :occupant-scape)
        coords (contents r :coords-scape)]
    (set! *print-level* 10)
    (testing "initialization"
      (is (= [(address-of m)] (s-> address->resolve (contents r :matrice-scape) :matrice)))
      )
    (testing "incorporate"
      (let [flower-address (s-> matrice->incorporate r :flower "http://images.com/flower.jpg" 0 0)
            chicken-address (s-> matrice->incorporate r :chicken "http://images.com/chicken.jpg" 0 1)]
        (are [x y] (= x y)
             (address-of (get-receptor r flower-address)) flower-address
             [[0 0]] (s-> address->resolve coords flower-address)
             [[0 1]] (s-> address->resolve coords chicken-address)
             flower-address (s-> key->resolve coords [0 0])
             )
        (is (= flower-address (s-> key->resolve coords [0 0])))
        (is (not= flower-address chicken-address))))
    (testing "failed door entrance"
      (is (thrown-with-msg? RuntimeException #"incorrect room password" (s-> door->enter r {:password "wrong" :name  "x" :data {:name "e"}})))
      )
    (let [
          o (--> door->enter u r {:password "password" :name "zippy" :data {:name "Eric H-B", :image "http://gravatar.com/userimage/x.jpg" :phone "123/456-7890"}})]
      (testing "door->enter"
        (is (agent-or-matrice? r (address-of u) (address-of o)))
        (is (agent-or-matrice? r (address-of m) (address-of o)))
        (is (not (agent-or-matrice? r (address-of u-art) (address-of o))))
        (is (= o (get-receptor r (address-of o))))
        ;; enter event is posted to the door log
        (let [le (last @(contents r :door-log))]
          (is (= "zippy" (:who le)))
          (is (= "entered" (:what le)))
          (is (instance? java.util.Date (:when le))))
        ;; sender of enter is in the agent scape for the occupant
        (is (= (address-of u) (s-> key->resolve (contents r :agent-scape) (address-of o))))
        (comment is (= (s-> key->resolve (contents r :seat-scape) 0) (address-of o)))
        (is (= (s-> key->all occupants) ["zippy"] ))
        (is (thrown-with-msg? RuntimeException #"'zippy' is already in the room" (s-> door->enter r {:password "password" :name  "zippy" :data {:name "e"}}))))
      (testing "door-leave"
        ;; refuse leave if not from agent
        (is (thrown-with-msg? RuntimeException #"no agency" (--> door->leave u-art r "zippy")))
        (--> door->leave u r "zippy")
        ;; leave event is posed to the door log
        (let [le (last @(contents r :door-log))]
            (is (= "zippy" (:who le)))
            (is (= "left" (:what le)))
            (is (instance? java.util.Date (:when le))))
        (comment is (= nil (contents (contents r :seat-scape) :map)))
        (is (= [] (s-> address->resolve occupants (address-of o))))
        (is (= (s-> key->all (contents r :occupant-scape)) [] ))
        (is (nil? (get-receptor r (address-of o))))
        ;; leave works if from matrice
        (--> door->enter u r {:password "password" :name "zippy" :data {:name "Eric"}})
        (--> door->leave m r "zippy")))
    (testing "move"
      (let [o (--> door->enter u r {:password "password" :name "zippy" :data {:name "Eric"}})
            addr (address-of o)]
        ;; refuse if not from matrice
        (is (thrown-with-msg? RuntimeException #"not matrice" (-->  matrice->move u r addr 100 100)))
        (--> matrice->move m r addr 100 100 )
        (is (= addr (s-> key->resolve coords [100 100])))
        (--> matrice->move m r addr 20 20)
        (is (= [[20 20]] (s-> address->resolve coords addr)))))
    (testing "talking-stick"
      (--> door->enter u-art r {:password "password" :name "art" :data {:name "Art"}})
      (let [f (contents r :talking-stick)
            s (contents f :stick-scape)
            zippy_addr (s-> key->resolve occupants "zippy")
            art_addr (s-> key->resolve occupants "art") ]
        ;; refuse if not from agent
        (is (thrown-with-msg? RuntimeException #"no agency" (--> stick->request u-art r "zippy")))
        (--> stick->request u r "zippy")
        (is (= [zippy_addr] (s-> address->resolve s :have-it)))
        (--> stick->request u-art r "art")
        (--> stick->release u r "zippy")
        (is (= [art_addr] (s-> address->resolve s :have-it)))
        (--> stick->give u r "zippy")
        (is (= [zippy_addr] (s-> address->resolve s :have-it)))
        ))
    (testing "state-pretty"
      ;; cant actually test the contents because addresses change...
      (is (= (into #{} (keys  (state r false)))
             #{:receptors :matrices :scapes :type :talking-stick :address})))
    (testing "state-full"
      (is (= (into #{} (keys  (state r true)))
             #{:scapes-scape-addr :receptors :type :address :door-log :door :matrice-scape :talking-stick :password}))
      (comment is (= nil (restore (state r true) nil))))
    (testing "restore"
      (is (=  (state r true) (state (restore (state r true) nil) true))))
    ))

(comment set-person-image )
(comment stick creates slots for every occupant in the room.  facilitation receptor)
