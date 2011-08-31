(ns
  #^{:author "Eric Harris-Braun"
     :doc "Channel receptor"}
  anansi.streamscapes.channel
  (:use [anansi.ceptr]
        [anansi.streamscapes.streamscapes]
        [anansi.receptor.scape])
  (:use [clj-time.core :only [now]]))

(defmethod manifest :channel [_r name]
           (make-scapes _r {:name name} :deliverer :receiver)
           )
(defmethod state :channel [_r full?]
           (assoc (state-convert _r full?)
             :name (contents _r :name)
             ))
(defmethod restore :channel [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :name (:name state))
             r))
(defn get-deliverer-bridge [_r]
  (--> key->resolve _r (get-scape _r :deliverer) :deliverer))
(defn get-receiver-bridge [_r]
  (--> key->resolve _r (get-scape _r :receiver) :receiver))

; the receive signal is called by the receiver bridge to create a new droplet
(signal stream receive [_r _f {id :id to :to from :from envelope :envelope content :content}]
        (rsync _r
               (let [ss (parent-of _r)
                     droplet-address (--> channel->incorporate _r ss {:id id :from from :to to :aspect (contents _r :name) :envelope envelope :content content})
                     ]
                 droplet-address)))

; the send signal calls the deliverer bridge to deliver a droplet
(signal stream send [_r _f params]
        (let [ss (parent-of _r)]
          (rsync ss
                 (let [droplet-address (:droplet-address params)
                       d (get-receptor ss droplet-address)
                       deliveries (get-scape ss :delivery)
                       aspect (contents _r :name)
                       [bridge-address delivery-signal] (get-deliverer-bridge _r)
                       errors (--> delivery-signal _r (get-receptor _r bridge-address) params)]
                   (if (nil? errors)
                     (--> key->set _r deliveries {:aspect aspect :time (str (now))} droplet-address))
                   errors))))

; hand a message to the bridge to be received
(signal bridge receive [_r _f message]
        (let [[bridge-address receive-signal] (get-receiver-bridge _r)
              b (get-receptor _r bridge-address)]
          (--> receive-signal _r b message)))
