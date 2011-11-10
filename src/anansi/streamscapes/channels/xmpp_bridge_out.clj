(ns
  #^{:author "Eric Harris-Braun"
     :doc "xmpp Out Bridge receptor"}
  anansi.streamscapes.channels.xmpp-bridge-out
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]
)
  (:use [clj-time.core :only [now]]))

(def xmpp-bridge-out-def (receptor-def "xmpp-bridge-out"))

(signal channel deliver [_r _f {droplet-address :droplet-address}]
        (let [
              parent-channel (parent-of _r)
              ss (parent-of parent-channel)
              d (get-receptor ss droplet-address)
              xmpp-contacts (get-scape ss :xmpp-address-contact)
              to-xmpp (first (--> address->resolve _r xmpp-contacts (contents d :to)))
              content (contents d :content)
              [controller-address control-signal] (get-controller parent-channel)]
          (--> (apply get-signal-function control-signal) _r controller-address {:command :send :params {:body (:body content) :thread (:thread content) :subject (:subject content) :type (:type content) :to to-xmpp}}))
        )

