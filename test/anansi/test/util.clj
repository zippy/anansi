(ns anansi.test.util
  (:use [anansi.util] :reload)
  (:use [midje.sweet]))

(fact (modify-keys inc {1 2 3 4}) => {2 2 4 4})
(fact (modify-vals inc {1 2 3 4}) => {1 3 3 5})
(let [r (ref {4 5})
      n (keyword (last (re-find #"@(.*)$" (str r))))]
  (fact (snapshot {:a 2, :b r}) => {:a 2, :b {n {4 5}}}))
