(ns ss.ss-utils
  (:require [ss.state :as s]
            [ss.debug :as debug]
            [ss.ceptr :as ceptr]
            ))

(defn send-signal
  "send signal to the host, inserting the current session into the params"
  ([params] (ceptr/signal (assoc params :session (s/get-session))))
  ([params callback] (ceptr/signal (assoc params :session (s/get-session)) callback)))


(defn send-ss-signal
  "send signal to the current session streamscapes instance"
  ([params] (ceptr/signal (assoc params :session (s/get-session) :to (s/get-ss-addr) :prefix "streamscapes.streamscapes")))
  ([params callback] (ceptr/signal (assoc params :session (s/get-session) :to (s/get-ss-addr) :prefix "streamscapes.streamscapes") callback)))

