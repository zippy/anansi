(ns
  #^{:author "Eric Harris-Braun"
     :doc "Channel receptor"}
  anansi.streamscapes.channel
  (:use [anansi.ceptr]
        [anansi.streamscapes.streamscapes]
        [anansi.receptor.scape])
  (:use [clj-time.core :only [now]]))

(def channel-def (receptor-def "channel"
                               (attributes :name)
                               (scapes :deliverer :receiver :controller)
                               ))

(defn get-channel-receptor [_r name]
  (--> key->resolve _r (get-scape _r name) name))

(defn get-controller [_r]
  (get-channel-receptor _r :controller))

(defn get-deliverer-bridge [_r]
  (get-channel-receptor _r :deliverer))

(defn get-receiver-bridge [_r]
  (get-channel-receptor _r :receiver))

; the receive signal is called by the receiver bridge to create a new droplet
(signal stream receive [_r _f {id :id to :to from :from sent :sent envelope :envelope content :content}]
        (rsync _r
               (let [ss (parent-of _r)
                    
                     xx (try (prn "INCORPORATING:" {:id id :from from :to to :sent sent :channel (contents _r :name) :envelope envelope :content content} )
                             (catch Exception e
                               (prn "ERROR " e)
                               :err
                               ))
                     yy (if (= xx :err) (do
                                           (doseq [ [k v] content] (prn "Key: " k) (prn "Value: " v))
                                           (prn "XXX" {:id id :from from :to to :channel (contents _r :name) :envelope envelope :content content})
                                           ))
                     droplet-address (--> channel->incorporate _r ss {:id id :from from :to to :channel (contents _r :name) :envelope envelope :content content})
                     receipts (get-scape ss :receipt)
                     deliveries (get-scape ss :delivery)
                     ]
                 (--> key->set _r receipts (str (now)) droplet-address)
                 (--> key->set _r deliveries (if (nil? sent) (str (now)) sent) droplet-address)
                 droplet-address)))

; the send signal calls the deliverer bridge to deliver a droplet
(signal stream send [_r _f params]
        (let [ss (parent-of _r)]
          (rsync ss
                 (let [droplet-address (:droplet-address params)
                       d (get-receptor ss droplet-address)
                       deliveries (get-scape ss :delivery)
                       channel (contents _r :name)
                       [bridge-address delivery-signal] (get-deliverer-bridge _r)
                       errors (--> delivery-signal _r (get-receptor _r bridge-address) params)]
                   (if (nil? errors)
                     (--> key->set _r deliveries (str (now)) droplet-address)
                     (prn "Delivery Errors: " errors))
                   errors))))

; hand a message to the bridge to be received
(signal bridge receive [_r _f message]
        (let [[bridge-address receive-signal] (get-receiver-bridge _r)
              b (get-receptor _r bridge-address)]
          (--> receive-signal _r b message)))

(signal stream control [_r _f params]
        (let [[controller-address control-signal] (get-controller _r)]
             (if (nil? controller-address)
               (throw (RuntimeException. (str "channel has no controller")))
               )
             (--> control-signal _r (get-receptor _r controller-address) params)))

