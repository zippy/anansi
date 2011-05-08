(ns anansi.test.receptor.scape
  (:use [anansi.receptor.scape] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest scape
  (let [s (receptor scape nil)]
    (key->set s :a 1)
    (key->set s :b 1)
    (testing "contents"
      (is (= @(contents s :map) {:a 1,:b 1})))
    (testing "setting and resolving"
      (is (= 1 (key->resolve s :a)))
      (is (= [:a :b] (address->resolve s 1)))
      (is (= [:a :b] (into [] (key->all s))))
      (is (= [1] (into [] (address->all s)))))
    (testing "removing"
        (key->delete s :a)
        (is (= nil (key->resolve s :a)))
        (is (= [:b] (address->resolve s 1)))
        (address->delete s 1)
        (is (= [] (address->resolve s 1))))
    (comment is (= {:type "HashScape", :contents {:a 1}} (scape-dump scape)))
    ))
