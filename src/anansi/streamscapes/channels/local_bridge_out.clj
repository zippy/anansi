(ns
  #^{:author "Eric Harris-Braun"
     :doc "Local Bridge Out receptor"}
  anansi.streamscapes.channels.local-bridge-out
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]))

(defmethod manifest :local-bridge-out [_r params]
           {})
(defmethod state :local-bridge-out [_r full?]
          (state-convert _r full?))
(defmethod restore :local-bridge-out [state parent]
  (let [r (do-restore state parent)]
    r))

(signal channel deliver [_r _f {droplet-address :droplet-address}] 
        (let [cc (parent-of _r)
              ss (parent-of  cc)
              d (get-receptor ss droplet-address)]
          (try
            (let [
                  ss-addr-idents (get-scape ss :ss-address-ident)
                  to-ss-addr (first (--> address->resolve _r ss-addr-idents (contents d :to)))
                  from-ss-addr (first (--> address->resolve _r ss-addr-idents (contents d :from)))
                  content (contents d :content)
                  envelope (contents d :envelope)
                  ]
              (--> streamscapes->receive ss to-ss-addr {:id (contents d :id) :from from-ss-addr :to to-ss-addr :aspect :local-stream :envelope envelope :content content})
              nil)
            (catch Exception e
              (str e)))
          ))