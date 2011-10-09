(ns ss.ss-utils
  (:require [ss.session :as s]
            [ss.debug :as debug]
            [ss.ceptr :as ceptr]
            ))

(defn send-signal
  "send a streamscapes signal, inserting the current session into the params"
  ([params] (ceptr/signal (assoc params :session (s/get-session))))
  ([params callback] (ceptr/signal (assoc params :session (s/get-session)) callback)))


(defn set-current-state
  "set the current streamscapes state for others to refer to it"
  [s]
  (def *current-state* s)
  )
