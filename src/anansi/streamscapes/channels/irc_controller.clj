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
  (:use [clj-time.core :only [now]]
        [clojure.contrib.json])
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader))
)


(declare conn-handler)

(defn connect [host port receiver]
  (let [socket (Socket. host (Integer. port))
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
       (println msg)
      (s-> controller->receive (:receiver @conn) msg)
      (cond 
       (re-find #"^ERROR :Closing Link:" msg) 
       (dosync (alter conn merge {:exit true}))
       (re-find #"^PING" msg)
       (write conn (str "PONG " (re-find #":.*" msg)))))))

(defn login [conn user nick]
  (write conn (str "NICK " nick))
  (write conn (str "USER " nick " 0 * :" user)))

(def irc-controller-def
     (receptor-def "irc-controller"
                   (attributes :host :port :user :nick)
                   (animate [_r reanimate]
                            (if (not reanimate)
                              (let [ss (parent-of (parent-of _r))
                                    irc-contacts (get-scape ss :irc-contact true)
                                    nick (contents _r :nick)]
                                (if (nil? (--> key->resolve _r irc-contacts nick))
                                  (--> matrice->identify _r ss {:identifiers {:irc nick} :attributes {:name (contents _r :name)}}))
                                )))))

(defn get-status [_r]
     (let [conn (:irc-connection @_r)]
       (if (or (nil? conn) (closed? conn)) :closed :open))
     )

(signal channel control [_r _f control-params]
        (let [{command :command params :params} control-params]
          (condp = command
              :status (get-status _r)
              :open (let [parent-channel (parent-of _r)
                          [in-bridge-address receive-signal] (get-receiver-bridge parent-channel)
                          ib (get-receptor parent-channel in-bridge-address)
                          conn (connect (contents _r :host) (contents _r :port) ib)]
                      (dosync (alter _r assoc :irc-connection conn))
                      (login conn (contents _r :user) (contents _r :nick))
                      nil
                      )
              :join (let [ss (parent-of (parent-of _r))
                          irc-channel (:channel params)
                          irc-contacts (get-scape ss :irc-contact true)]
                      (if (nil? (--> key->resolve _r irc-contacts irc-channel))
                        (--> matrice->identify _r ss {:identifiers {:irc irc-channel} :attributes {:name (str "IRC channel: " irc-channel)}}))
                      (write (:irc-connection @_r) (str "JOIN " irc-channel))
                        nil)
              :close (do
                       (if (= :closed (get-status _r)) (throw (RuntimeException. "Channel not open")))
                       (write (:irc-connection @_r) "QUIT")
                         (dosync (alter _r dissoc :irc-connection))
                         nil
                         )
              :msg (do (write (:irc-connection @_r) (str "PRIVMSG " (:to params) " :" (:message params)))
                       nil)
              (throw (RuntimeException. (str "Unknown control command: " command))))))

(defn- write-json-clojure-lang-var [x #^PrintWriter out]
    (.print out (json-str (str x)))) ;; or something useful here!

(extend clojure.lang.Var clojure.contrib.json/Write-JSON
        {:write-json write-json-clojure-lang-var})

