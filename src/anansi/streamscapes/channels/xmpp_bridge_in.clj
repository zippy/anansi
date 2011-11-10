(ns
  #^{:author "Eric Harris-Braun"
     :doc "xmpp Bridge receptor"}
  anansi.streamscapes.channels.xmpp-bridge-in
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel])
  (:use [clj-time.core :only [now]]))

(def xmpp-bridge-in-def (receptor-def "xmpp-bridge-in"))

(defn handle-message [_r msg]
  "process an xmpp message"
  (let [id (:packet-id msg)
        ss (parent-of (parent-of _r))
        ids (get-scape ss :id)
        da (s-> address->resolve ids id)]
    (if (empty? da)
      (let [
            from-id (do-identify ss {:identifiers {:xmpp-address (:from msg)}} false)
            to-id (do-identify ss {:identifiers {:xmpp-address (:to msg)}} false)]
        (--> stream->receive _r (parent-of _r)
             {:id id
              :to to-id
              :from from-id
              :envelope {:from "address/xmpp" :to "address/xmpp" :body "text/plain" :subject "text/plain" :thread "thread/xmpp" :error "error/xmpp" :type "message-type/xmpp"}
              :content msg}))
      (first da))))

(signal controller receive [_r _f msg]
        (handle-message _r msg))
