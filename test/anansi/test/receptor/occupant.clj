(ns anansi.test.receptor.occupant
  (:use [anansi.receptor.occupant] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(comment deftest occupant
  (let [s (receptor occupant)]
    (testing "contents"
      (is (= @(contents s) {:a 1,:b 1})))
    (testing "signal"
      (is (= 1 (self->new s ))))

    ))
