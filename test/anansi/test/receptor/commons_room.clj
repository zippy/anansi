(ns anansi.test.receptor.commons-room
  (:use [anansi.receptor.commons-room] :reload)
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [clojure.test]))

(deftest commons-room
  (let [r (receptor commons-room nil)
        occupants (contents r :occupant-scape)
        coords (contents r :coords-scape)]
    (set! *print-level* 4)
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
    (testing "door"
      (let [o (s-> door->enter r {:name "zippy" :data {:name "Eric H-B", :image "http://gravatar.com/userimage/x.jpg" :phone "123/456-7890"}})]
        (is (= o (get-receptor r (address-of o))))
        (let [le (last @(contents r :door-log))]
          (is (= "zippy" (:who le)))
          (is (= "entered" (:what le)))
          (is (instance? java.util.Date (:when le))))
        (comment is (= (s-> key->resolve (contents r :seat-scape) 0) (address-of o)))
        (is (= (s-> key->all occupants) ["zippy"] ))
        (is (thrown? RuntimeException (door->enter r {:name  "zippy" :data {:name "e"}})))
        (s-> door->leave r "zippy")
        (let [le (last @(contents r :door-log))]
          (is (= "zippy" (:who le)))
          (is (= "left" (:what le)))
          (is (instance? java.util.Date (:when le))))
        (comment is (= nil (contents (contents r :seat-scape) :map)))
        (is (= [] (s-> address->resolve occupants (address-of o))))
        (is (= (s-> key->all (contents r :occupant-scape)) [] ))
        (is (nil? (get-receptor r (address-of o))))))
    (testing "move"
      (let [o (s-> door->enter r {:name "zippy" :data {:name "Eric"}})
            addr (address-of o)]
        (s-> matrice->move r addr 100 100 )
        (is (= addr (s-> key->resolve coords [100 100])))
        (s-> matrice->move r addr 20 20)
        (is (= [[20 20]] (s-> address->resolve coords addr)))))
    (testing "talking-stick"
      (s-> door->enter r {:name "art" :data {:name "Art"}})
      (let [f (contents r :talking-stick)
            s (contents f :stick-scape)
            zippy_addr (s-> key->resolve occupants "zippy")
            art_addr (s-> key->resolve occupants "art") ]
        (s-> stick->request r "zippy")
        (is (= [zippy_addr] (s-> address->resolve s :have-it)))
        (s-> stick->request r "art")
        (s-> stick->release r "zippy")
        (is (= [art_addr] (s-> address->resolve s :have-it)))
        (s-> stick->give r "zippy")
        (is (= [zippy_addr] (s-> address->resolve s :have-it)))
        ))))

(comment set-person-image )
(comment stick creates slots for every occupant in the room.  facilitation receptor)
