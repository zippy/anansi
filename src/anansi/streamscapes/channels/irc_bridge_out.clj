(ns
  #^{:author "Eric Harris-Braun"
     :doc "IRC Out Bridge receptor"}
  anansi.streamscapes.channels.irc-bridge-out
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]
)
  (:use [clj-time.core :only [now]]))

(def irc-bridge-out-def (receptor-def "irc-bridge-out"))

(signal channel deliver [_r _f {droplet-address :droplet-address}]
        (let [
              parent-channel (parent-of _r)
              ss (parent-of parent-channel)
              d (get-receptor ss droplet-address)
              irc-idents (get-scape ss :irc-ident)
              to-irc (first (--> address->resolve _r irc-idents (contents d :to)))
              content (contents d :content)
              [controller-address control-signal] (get-controller parent-channel)]
          (--> control-signal _r controller-address {:command :msg :params {:message (:message content) :to to-irc}}))
        )
