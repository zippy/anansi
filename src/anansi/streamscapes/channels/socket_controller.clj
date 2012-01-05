(ns
  #^{:author "Eric Harris-Braun"
     :doc "TCP/IP Socket Controller receptor"}
  anansi.streamscapes.channels.socket-controller
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]
        )
  (:use [clj-time.core :only [now]])
  (:use [clojure.java.io :only [reader writer]]
        [anansi.libs.server-socket :only [create-server close-server]])
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader))
)

(def *done*)
(def *msg*)
(defn- cleanup []
  "cleanup actions?"
  (println "SOCKET CLOSING!")
  )

(defn make-handle-connection [_r]
  (fn [#^Socket s in out]
    (let [from-ip (.getHostAddress (.getInetAddress s))]
      (binding [*in* (reader in)
                *out* (writer out)]

        ;; We have to nest this in another binding call instead of using
        ;; the one above so *in* and *out* will be bound to the socket
    
        (binding [*done* false
                  *msg* (ref "")]
          (try (loop [input (read-line)]
                 (when input
                   (if (= input ".")
                     (do (--> bridge->receive _r (parent-of _r) {:from from-ip :to "127.0.0.1" :message @*msg*})
                         (dosync (ref-set *msg* ""))
                         )
                     (dosync (ref-set *msg* (str @*msg* input "\n"))))
                   (if (not *done*) (recur (read-line)) )
                   ))
               (finally (cleanup))))))
    )) 
(def socket-controller-def (receptor-def "socket-controller" (attributes :port :input-function)))

(defn closed? [conn]
  (.isClosed (:server-socket conn)))

(defn get-status [_r]
     (let [conn (:socket @_r)]
       (if (or (nil? conn) (closed? conn)) :closed :open))
     )
(signal channel control [_r _f control-params]
        (let [{command :command params :params} control-params]
          (condp = command
              :status (get-status _r)
              :open (let [listener (create-server (Integer. (contents _r :port))
                                                  (make-handle-connection _r))]
                      (dosync (alter _r assoc :socket listener))
                      nil)
              :close (do
                       (if (= :closed (get-status _r)) (throw (RuntimeException. "Channel not open")))
                       (close-server (:socket @_r))
                       (dosync (alter _r dissoc :socket))
                       nil
                         )
              (throw (RuntimeException. (str "Unknown control command: " command))))))
