(ns
  #^{:author "Eric Harris-Braun"
     :doc "Email Bridge receptor"}
  anansi.streamscapes.bridge-local-in
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]))

(defmethod manifest :bridge-local-in [_r params]
           {})
(defmethod state :bridge-local-in [_r full?]
           (state-convert _r full?))
(defmethod restore :bridge-local-in [state parent]
           (let [r (do-restore state parent)]
             r))


(defn handle-message [_r message]
  "process a locally received message: do  look-up to see if we've already created a droplet for this id, and also map the streamscapes-id to/from addresses into identities."
  (let [id {:id message}
        ss (parent-of (parent-of _r))
        ids (get-scape ss :id)
        da (s-> address->resolve ids id)]
    (if (empty? da)
      (let [to (:to message)
            from (:from message)
            to-id (do-identify ss {:identifiers {:ss-address to}} false)
            from-id (do-identify ss {:identifiers {:ss-address from}} false)]
        (--> stream->receive _r (parent-of _r)
             {:id id
              :to to-id
              :from from-id
              :envelope (:envelope message)
              :content (:content message)}))
      (first da))))

