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
      (let [flower-address (matrice->incorporate r :flower "http://images.com/flower.jpg" 0 0)
            chicken-address (matrice->incorporate r :chicken "http://images.com/chicken.jpg" 0 1)]
        (is (= (address-of (get-receptor r flower-address)) flower-address))
        (is (= [[0 0]] (address->resolve coords flower-address)))
        (is (= flower-address (key->resolve coords [0 0])))
        (is (not= flower-address chicken-address))))
    (testing "door"
      (let [o (door->enter r "zippy" {:name "Eric H-B", :image "http://gravatar.com/userimage/x.jpg" :phone "123/456-7890"})]
        (is (= o (get-receptor r (address-of o))))
        (let [le (last @(contents r :door-log))]
          (is (= "zippy" (:who le)))
          (is (= "entered" (:what le)))
          (is (instance? java.util.Date (:when le))))
        (comment is (= (key->resolve (contents r :seat-scape) 0) (address-of o)))
        (is (= (key->all occupants) ["zippy"] ))
        (is (thrown? RuntimeException (door->enter r "zippy" {:name "e"})))
        (door->leave r "zippy")
        (let [le (last @(contents r :door-log))]
          (is (= "zippy" (:who le)))
          (is (= "left" (:what le)))
          (is (instance? java.util.Date (:when le))))
        (comment is (= nil (contents (contents r :seat-scape) :map)))
        (is (= [] (address->resolve occupants (address-of o))))
        (is (= (key->all (contents r :occupant-scape)) [] ))))
    (testing "move"
      (let [o (door->enter r "zippy" {:name "Eric"})
            addr (address-of o)]
        (matrice->move r addr 100 100 )
        (is (= addr (key->resolve coords [100 100])))
        (matrice->move r addr 20 20)
        (is (= [[20 20]] (address->resolve coords addr)))))
    ))
