(ns anansi.test.receptor.host
  (:use [anansi.receptor.host] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape])
  (:use [clojure.test]))

(deftest host
  (let [h (receptor host nil)]
    (testing "host-room"
      (let [addr (s-> self->host-room h {:name "the room" :password "passord" :matrice-address 1})]
        (is (= addr (s-> key->resolve (contents h :room-scape) "the room"))))
      )
    (testing "host-user"
      (let [addr (s-> self->host-user h "zippy")]
        (is (= ["zippy"] (s-> key->all (contents h :user-scape))))
        (is (= addr (s-> self->host-user h "zippy")))))
    ))
