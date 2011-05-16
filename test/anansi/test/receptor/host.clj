(ns anansi.test.receptor.host
  (:use [anansi.receptor.host] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape])
  (:use [clojure.test]))

(deftest host
  (let [h (receptor host nil)]
    (testing "host-room"
      (let [addr (s-> self->host-room h {:name "the room" :password "pass" :matrice-address 1})
            r (get-receptor h addr)]
        (is (= addr (s-> key->resolve (contents h :room-scape) "the room")))
        (is (= (contents r :password) "pass") )
        (is (= (s-> key->all (get-scape r :matrice-scape)) [1]) )
        )
      )
    (testing "host-user"
      (let [addr (s-> self->host-user h "zippy")
            u (get-receptor h addr)]
        (is (= ["zippy"] (s-> key->all (contents h :user-scape))))
        (is (= addr (s-> self->host-user h "zippy")))
        (is (= (contents u :name) "zippy"))))
    ))
