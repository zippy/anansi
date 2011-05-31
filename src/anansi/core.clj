(ns
    #^{:author "Eric Harris-Braun"
       :doc "A reference implementation of the ceptr platform"}
  anansi.core
  (:use [anansi.server :only [launch-server]]
        [anansi.web-server :only [launch-web-server]]))

(defn -main
  ([cl-port web-port]
     (do 
       (launch-web-server web-port)       
       (launch-server cl-port)))
  
  ([] (-main 3333 8080)))

