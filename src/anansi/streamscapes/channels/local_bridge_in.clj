(ns
  #^{:author "Eric Harris-Braun"
     :doc "Email Bridge receptor"}
  anansi.streamscapes.channels.local-bridge-in
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]))

(def local-bridge-in-def (receptor-def "local-bridge-in"))

(defn handle-message [_r message]
  "process a locally received message: do  look-up to see if we've already created a droplet for this id, and also map the streamscapes-id to/from addresses into contacts."
  (let [id (:id message)
        ss (parent-of (parent-of _r))
        ids (get-scape ss :id)
        da (s-> address->resolve ids id)]
    (if (empty? da)
      (let [to (:to message)
            from (:from message)
            to-id (do-identify ss {:identifiers {:streamscapes-address to}} false)
            from-id (do-identify ss {:identifiers {:streamscapes-address from}} false)]
        (--> stream->receive _r (parent-of _r)
             {:id id
              :to to-id
              :from from-id
              :envelope (:envelope message)
              :content (:content message)}))
      (first da))))

(signal cheat receive [_r _f message]
        (handle-message _r message))
