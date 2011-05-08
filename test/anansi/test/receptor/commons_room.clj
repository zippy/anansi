(ns anansi.test.receptor.commons-room
  (:use [anansi.receptor.commons-room] :reload)
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [clojure.test]))

(deftest commons-room
  (let [r (receptor commons-room nil)]
    (testing "incorporate"
      (let [flower-address (matrice->incorporate r :flower "http://images.com/flower.jpg" 0 0)
            chicken-address (matrice->incorporate r :chicken "http://images.com/chicken.jpg" 0 1)
            coords-scape (get-receptor r 1)]
        (is (= (address-of (get-receptor r flower-address)) flower-address))
        (is (= [[0 0]] (address->resolve coords-scape flower-address)))
        (is (= flower-address (key->resolve coords-scape [0 0])))
        (is (not= flower-address chicken-address))))
    ))
