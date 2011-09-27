(ns
  #^{:author "Eric Harris-Braun"
     :doc "IRC Controller receptor"}
  anansi.streamscapes.channels.irc-controller
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]
        [anansi.streamscapes.channels.irc-bridge-in :only [controller->receive]]
       ; [anansi.streamscapes.channels.irc]
        )
  (:use [clj-time.core :only [now]])
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader))
)


(declare conn-handler)

(defn connect [host port receiver]
  (let [socket (Socket. host port)
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:in in :out out :socket socket :receiver receiver})]
    (doto (Thread. #(conn-handler conn)) (.start))
    conn))

(defn write [conn msg]
  
  (doto (:out @conn)
    (.println (str msg "\r"))
    (.flush)))

(defn closed? [conn]
  (or (:exit @conn) (.isClosed (:socket @conn))))

(defn conn-handler [conn]
  (while 
      (nil? (:exit @conn))
    (let [msg (.readLine (:in @conn))
          ;[_1 _2 from-address command _3 params] (re-find #"^(:([^ ]*) )*([a-zA-Z]*)( (.*))*$" msg)
          ]
      ; (println msg)
      (s-> controller->receive (:receiver @conn) msg)
      (cond 
       (re-find #"^ERROR :Closing Link:" msg) 
       (dosync (alter conn merge {:exit true}))
       (re-find #"^PING" msg)
       (write conn (str "PONG " (re-find #":.*" msg)))))))

(defn login [conn user nick]
  (write conn (str "NICK " nick))
  (write conn (str "USER " nick " 0 * :" user)))

(def irc-controller-def (receptor-def "irc-controller"
                                      (attributes :host :port :user :nick)))

(signal channel control [_r _f control-params]
          (let [{command :command params :params} control-params]
            (condp = command
                :status (let [conn (:irc-connection @_r)]
                          (if (or (nil? conn) (closed? conn)) :closed :open))
                :open (let [parent-channel (parent-of _r)
                            [in-bridge-address receive-signal] (get-receiver-bridge parent-channel)
                            ib (get-receptor parent-channel in-bridge-address)
                            conn (connect (contents _r :host) (contents _r :port) ib)]
                        (dosync (alter _r assoc :irc-connection conn))
                        (login conn (contents _r :user) (contents _r :nick))
                        )
                :join (write (:irc-connection @_r) (str "JOIN " (:channel params)))
                :close (do (write (:irc-connection @_r) "QUIT")
                           (dosync (alter _r dissoc :irc-connection))
                           )
                :msg (write (:irc-connection @_r) (str "PRIVMSG " (:to params) " :" (:message params)))
              (throw (RuntimeException. (str "Unknown control command: " command))))))
