(ns anansi.test.receptor.host
  (:use [anansi.receptor.host] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape])
  (:use [clojure.test]))

(deftest host
  (let [h (receptor host nil)]
    (testing "host-room"
      (let [addr (self->host-room h "the room")]
        (is (= addr (key->resolve (contents h :room-scape) "the room"))))
      )
    (testing "host-user"
      (let [addr (self->host-user h "zippy")]
        (is (= ["zippy"] (key->all (contents h :user-scape))))
        (is (= addr (self->host-user h "zippy")))))
    ))
