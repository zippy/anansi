(ns
    #^{:author "Eric Harris-Braun"
       :doc "A reference implementation of the ceptr platform"}
  anansi.core
  (:use
   [anansi.ceptr]
   [anansi.receptor.host]
   [anansi.receptor.host-interface.http]
   [anansi.server :only [launch-server]]
        [anansi.web-server :only [launch-web-server]]))

(defn -main
  ([cl-port web-port]
     (let [h (receptor :host nil)
           i (receptor :http-host-interface h {:auto-start {:port web-port}})]
       (println (str "Starting web interface on port " web-port))
       (while true (Thread/sleep 10000))
       ;(launch-web-server web-port)       
       ;(launch-server cl-port)
       ))
  
  ([] (-main 3333 8080)))

