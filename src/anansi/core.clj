(ns
  #^{:author "Eric Harris-Braun"
     :doc "A reference implementation of the ceptr platform"}
  anansi.core
  (:require anansi.streamscapes.channels.irc-controller)
  (:require anansi.streamscapes.channels.irc-bridge-out)
  (:require anansi.streamscapes.channels.xmpp-controller)
  (:require anansi.streamscapes.channels.xmpp-bridge-out)
  (:require anansi.streamscapes.channels.xmpp-bridge-in)
  (:require anansi.streamscapes.channels.local-bridge-in)
  (:require anansi.streamscapes.channels.local-bridge-out)
  (:require anansi.streamscapes.channels.email-bridge-in)
  (:require anansi.streamscapes.channels.email-bridge-out)
  (:require anansi.streamscapes.channels.email-controller)
  (:require anansi.streamscapes.channels.twitter-bridge-in)
  (:require anansi.streamscapes.channels.twitter-controller)
  (:use [anansi.ceptr]
        [anansi.receptor.host]
        [anansi.receptor.host-interface.http :only [http-def]]
        [anansi.server :only [launch-server]]
        [anansi.web-server :only [launch-web-server]]))
(set! *print-level* 999)
(defn -main
  ([cl-port web-port verbose]
    (if (not (load-receptors))
      (let [h (make-receptor host-def nil {})]
        (make-receptor http-def h {:auto-start {:port web-port}})))
    (if verbose (dosync (ref-set *log-level* :verbose)))
    (let [x (ref 0)]
      (while true (do (if (not= @x @*changes*)
                        (do
;                          (prn "serializing" @x @*changes*)
                          (spit *server-state-file-name* (serialize-receptors *receptors*))
                          (dosync (ref-set x @*changes*))))
                    (Thread/sleep 1000)))))

  ;       (while true (Thread/sleep 10000))
  ;(launch-web-server web-port)
  ;(launch-server cl-port)
  ([verbose] (-main 3333 8080 verbose))
  ([] (-main 3333 8080 false)))

