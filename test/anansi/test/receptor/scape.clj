(ns anansi.test.receptor.scape
  (:use [anansi.receptor.scape] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest scape
  (let [s (receptor scape nil)]
    (s-> key->set s :a 1)
    (s-> key->set s :b 1)
    (testing "contents"
      (is (= @(contents s :map) {:a 1,:b 1})))
    (testing "setting and resolving"
      (is (= 1 (s-> key->resolve s :a)))
      (is (= [:a :b] (s-> address->resolve s 1)))
      (is (= [:a :b] (into [] (s-> key->all s))))
      (is (= [1] (into [] (s-> address->all s)))))
    (testing "removing"
        (s-> key->delete s :a)
        (is (= nil (s-> key->resolve s :a)))
        (is (= [:b] (s-> address->resolve s 1)))
        (s-> address->delete s 1)
        (is (= [] (s-> address->resolve s 1))))
    (comment is (= {:type "HashScape", :contents {:a 1}} (scape-dump scape)))
    ))

(deftest make-scapes-test
  (testing "make-scapes"
    (set! *print-level* 6)
    (let [r (receptor test-receptor nil)
          m (make-scapes r {:x :y} :a :b)]
      (is (= #{:x :a-scape :b-scape :scapes-scape} (into #{} (keys m))))
      (is ()))
    ))
