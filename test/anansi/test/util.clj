(ns anansi.test.util
  (:use [anansi.util] :reload)
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]))

(deftest modify-keys-test
  (is (= {2 2 4 4} (modify-keys inc {1 2 3 4}))))

(deftest snapshot-test
  (is (= {:a 2, :b {4 5}}
         (snapshot {:a 2, :b (ref {4 5})}))))
