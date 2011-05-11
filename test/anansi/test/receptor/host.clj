(ns anansi.test.receptor.host
  (:use [anansi.receptor.host] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape])
  (:use [clojure.test]))

(deftest host
  (let [h (receptor host nil)]
    (testing "host"
        (let [addr (self->host h "the room")]
          (is (= addr (key->resolve (contents h :name-scape) "the room"))))
      )))
