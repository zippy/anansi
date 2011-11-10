(ns
  #^{:author "Eric Harris-Braun"
     :doc "Local Bridge Out receptor"}
  anansi.streamscapes.channels.local-bridge-out
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]))

(def local-bridge-out-def (receptor-def "local-bridge-out"))

(signal channel deliver [_r _f {droplet-address :droplet-address}] 
        (let [cc (parent-of _r)
              ss (parent-of  cc)
              d (get-receptor ss droplet-address)]
          (try
            (let [
                  ss-addr-contacts (get-scape ss :streamscapes-address-contact)
                  to-ss-addr (first (--> address->resolve _r ss-addr-contacts (contents d :to)))
                  from-ss-addr (first (--> address->resolve _r ss-addr-contacts (contents d :from)))
                  content (contents d :content)
                  envelope (contents d :envelope)
                  ]
              (--> streamscapes->receive ss to-ss-addr {:id (contents d :id) :from from-ss-addr :to to-ss-addr :channel (contents cc :name) :envelope envelope :content content})
              nil)
            (catch Exception e
              (str e)))
          ))
