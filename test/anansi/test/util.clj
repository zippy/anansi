(ns anansi.test.util
  (:use [anansi.util] :reload)
  (:use [midje.sweet])
  (:use [clj-time.core :only [date-time]]))

(fact (modify-keys inc {1 2 3 4}) => {2 2 4 4})
(fact (modify-vals inc {1 2 3 4}) => {1 3 3 5})
(let [r (ref {4 5})
      n (keyword (last (re-find #"@(.*)$" (str r))))]
  (fact (snapshot {:a 2, :b r}) => {:a 2, :b {n {4 5}}}))

(facts "about filter-map"
  (filter-map {:a 2, :b 3, :c {:d 4 :e 5}} {:a true}) => {:a 2}
  (filter-map {:a 2, :b 3, :c {:d 4 :e 5}} {:b true :c {:d true}}) => {:b 3 :c {:d 4}})

(facts "about standard-date-string"
  (standard-date-string "Wed Oct 04 09:21:40 +0000 2011") => "2011-10-04T03:21:40.000Z"
  (standard-date-string (date-time 2011 12 10 9 8 7)) => "2011-12-10T09:08:07.000Z"
  (standard-date-string (java.util.Date. "Wed Oct 04 09:21:40 +0000 2011")) => "2011-10-04T03:21:40.000Z"
  )
