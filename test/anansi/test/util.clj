(ns anansi.test.util
  (:use [anansi.util] :reload)
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]))

(deftest modify-keys-test
  (is (= {2 2 4 4} (modify-keys inc {1 2 3 4}))))

(deftest modify-vals-test
  (is (= {1 3 3 5} (modify-vals inc {1 2 3 4}))))

(deftest snapshot-test
  (let [r (ref {4 5})
        n (keyword (last (re-find #"@(.*)$" (str r))))]
    (is (= {:a 2, :b {n {4 5}}}
           (snapshot {:a 2, :b r})))))
