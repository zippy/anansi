(ns
  #^{:author "Eric Harris-Braun"
     :doc "xmpp Controller receptor"}
  anansi.streamscapes.channels.xmpp-controller
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]
        [anansi.streamscapes.channels.xmpp-bridge-in :only [controller->receive]]
       ; [anansi.streamscapes.channels.xmpp]
        )
  (:use [clj-time.core :only [now]]
        [clojure.contrib.json])
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader))
  (:import [org.jivesoftware.smack 
	    Chat 
	    ChatManager 
	    ConnectionConfiguration 
	    MessageListener
	    SASLAuthentication
	    XMPPConnection
	    XMPPException
	    PacketListener
            ]
	   [org.jivesoftware.smack.packet Message Presence Presence$Type Message$Type]
	   [org.jivesoftware.smack.filter MessageTypeFilter]
	   [org.jivesoftware.smack.util StringUtils])
)


(defonce *chat-message-type-filter* (MessageTypeFilter. Message$Type/chat))
(defonce *available-presence* (Presence. Presence$Type/available))
(defonce *unavailable-presence* (Presence. Presence$Type/unavailable))

(declare packet-listener)
(defn connect [host domain username password receiver]
  (let [connect-config (ConnectionConfiguration. host 5222 domain)
	conn (XMPPConnection. connect-config)]
    (.connect conn)
    (.login conn username password)
    (.addPacketListener conn (packet-listener conn receiver) *chat-message-type-filter*)
    conn))

(defn- msg-type [type]
  (condp = type
      :chat Message$Type/chat
      :normal Message$Type/normal
      :error Message$Type/error
      :groupchat Message$Type/groupchat
      :headline Message$Type/headline
      Message$Type/chat
      ))

(defn- send-message [conn message]
  (let [m (Message.)]
    (doto m
      (.setTo (:to message))
      (.setBody (:body message))
      (.setType (msg-type (:type message))))
    (.sendPacket conn m)))

(defn closed? [conn]
  (not (.isConnected conn)))

(defn- mapify-error [e]
  (if (nil? e) 
    nil
    {:code (.getCode e) :message (.getMessage e)}))

(defn- mapify-message [#^Message m]
  (try
   {:body (.getBody m)
    :subject (.getSubject m)
    :thread (.getThread m)
    :from(StringUtils/parseBareAddress (.getFrom m))
    :to (.getTo m)
    :packet-id (.getPacketID m)
    :error (mapify-error (.getError m))
    :type (keyword (str (.getType m)))}
   (catch Exception e (println e) {})))

(defn packet-listener [conn receiver]
     (proxy 
	 [PacketListener] 
	 []
       (processPacket [packet]
                      (let [msg (mapify-message #^Message packet)]
                        (if (and (:body msg) (:subject msg))
                          (s-> controller->receive receiver )
                          ))
                      )))

(def xmpp-controller-def
     (receptor-def "xmpp-controller"
                   (attributes :host :domain :username :_password)
                   (animate [_r reanimate]
                            (if (not reanimate)
                              (let [ss (parent-of (parent-of _r))
                                    xmpp-contacts (get-scape ss :xmpp-address-contact true)
                                    username (contents _r :username)]
                                (if (nil? (--> key->resolve _r xmpp-contacts username))
                                  (--> matrice->identify _r ss {:identifiers {:xmpp-address username} :attributes {:name (contents _r :username)}}))
                                )))))

(defn get-status [_r]
     (let [conn (:xmpp-connection @_r)]
       (if (or (nil? conn) (closed? conn)) :closed :open))
     )

(signal channel control [_r _f control-params]
        (let [{command :command params :params} control-params]
          (condp = command
              :status (get-status _r)
              :open (let [parent-channel (parent-of _r)
                          [in-bridge-address receive-signal] (get-receiver-bridge parent-channel)
                          ib (get-receptor parent-channel in-bridge-address)
                          conn (connect (contents _r :host) (contents _r :domain) (contents _r :username) (contents _r :_password) ib)]
                      (rsync _r (alter _r assoc :xmpp-connection conn))
                      nil
                      )
              :join (comment let [ss (parent-of (parent-of _r))
                          xmpp-channel (:channel params)
                          xmpp-contacts (get-scape ss :xmpp-address-contact true)]
                      (if (nil? (--> key->resolve _r xmpp-contacts xmpp-channel))
                        (--> matrice->identify _r ss {:identifiers {:xmpp-address xmpp-channel} :attributes {:name (str "xmpp channel: " xmpp-channel)}}))
                      (write (:xmpp-connection @_r) (str "JOIN " xmpp-channel))
                        nil)
              :close (do
                       (if (= :closed (get-status _r)) (throw (RuntimeException. "Channel not open")))
                       (.disconnect (:xmpp-connection @_r) *unavailable-presence*)
                       (rsync _r (alter _r dissoc :xmpp-connection))
                       nil
                       )
              :send (do (send-message (:xmpp-connection @_r) params)
                       nil)
              (throw (RuntimeException. (str "Unknown control command: " command))))))
