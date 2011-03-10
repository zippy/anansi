(ns anansi.test.map-utilities
  (:use [anansi.map-utilities] :reload)
  (:use [clojure.test]))

(deftest map-utilities
  (testing "remove"
    (is (= {:b 2} (remove-values {:a 1 :b 2 :c 3} [1 3])))
    (is (= {:b 2 :c 3} (remove-value {:a 1 :b 2 :c 3} 1))))
  (testing "get-keys"
    (is (= [:a :c] (get-keys {:a 1 :b 2 :c 1} 1))))
  )
