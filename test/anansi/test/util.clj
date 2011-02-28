(ns anansi.test.util
  (:use [anansi.util])
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]))

(deftest modify-keys-test
  (is (= {2 2 4 4} (modify-keys inc {1 2 3 4}))))
