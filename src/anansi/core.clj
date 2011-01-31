(ns
  #^{:author "Eric Harris-Braun"
     :doc "A reference implementation of the ceptr platform"}
  anansi.core
  (:use [anansi.server :only [launch-server]]))

(defn -main
  ([port]
     (launch-server port))
  ([] (-main 3333)))
