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
                    
                     xx (if (log-level-verbose) ( try (prn "INCORPORATING:" {:id id :from from :to to :sent sent :channel (contents _r :name) :envelope envelope :content content} )
                                                      (catch Exception e
                                                        (prn "ERROR " e)
                                                        :err
                                                        )))
                     yy (comment if (= xx :err) (do
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

(defn- getsig [n a s]
  (let [f (get-signal-function n a s)]
    (if (nil? f)
      (throw (RuntimeException. (str "Couldn't find signal:" [n a s])))
      f))
  )
; the send signal calls the deliverer bridge to deliver a droplet
(signal stream send [_r _f params]
        (let [ss (parent-of _r)]
          (rsync ss
                 (let [droplet-address (:droplet-address params)
                       d (get-receptor ss droplet-address)
                       deliveries (get-scape ss :delivery)
                       channel (contents _r :name)
                       [bridge-address [namespace aspect signal]] (get-deliverer-bridge _r)
                       errors (--> (getsig namespace aspect signal) _r (get-receptor _r bridge-address) params)]
                   (if (nil? errors)
                     (--> key->set _r deliveries (str (now)) droplet-address)
                     (prn "Delivery Errors: " errors))
                   errors))))

; hand a message to the bridge to be received
(signal bridge receive [_r _f message]
        (let [[bridge-address [namespace aspect signal]] (get-receiver-bridge _r)
              b (get-receptor _r bridge-address)]
          (--> (getsig namespace aspect signal) _r b message)))

(signal stream control [_r _f params]
        (let [[controller-address [namespace aspect signal]] (get-controller _r)]
             (if (nil? controller-address)
               (throw (RuntimeException. (str "channel has no controller")))
               )
             (--> (getsig namespace aspect signal) _r (get-receptor _r controller-address) params)))

(defn blank? [x] (or (= x nil) (= x "")))

;; TODO again more bogusness here around the types.  Also setting
;; content of a receptor from outside that receptor is also bogus.
(signal setup update-by-type [_r _f type params]
        (do
          (set-content _r :name (:name params))
          (condp = type
              :xmpp (let [[controller-address _] (get-controller _r)
                         controller (get-receptor _r controller-address)]
                      (doseq [key [:host :username :domain :password]]
                        (let [value (key params)
                              store-key (if (= key :password) :_password key)]
                          (if (not (blank? value))
                            (set-content controller store-key value)
                            ))))
              :irc (let [[controller-address _] (get-controller _r)
                         controller (get-receptor _r controller-address)]
                     (doseq [key [:host :nick :port :user]]
                       (if (not (blank? (key params))) (set-content controller key (key params))))
                     )
              :streamscapes nil
              :twitter (let [[controller-address _] (get-controller _r)
                             controller (get-receptor _r controller-address)]
                         (doseq [key [:search-query]]
                           (if (not (blank? (key params))) (set-content controller key (key params))))
                         )
              :socket (let [[controller-address _] (get-controller _r)
                             controller (get-receptor _r controller-address)]
                         (doseq [key [:port]]
                           (if (not (blank? (key params))) (set-content controller key (key params))))
                         )
              :email (let [[out-address _] (get-deliverer-bridge _r)
                           [in-address _] (get-receiver-bridge _r)
                           in (get-receptor _r in-address)
                           out (get-receptor _r out-address)]
                     (doseq [key [:host :account :password :protocol :port]]
                       (if (and (contains? params :in) (not (blank? (key (:in params))))) (set-content in key (key (:in params))))
                       (if (and (contains? params :out) (not (blank? (key (:out params))))) (set-content out key (key (:out params)))))
                     )
              ))
        )

(defn create-contact-on-animate
  "helper function to add address to a contact when a channel is created.  Assumes that _r is a sub receptor of the channel and has a :contact-address attribute"
  [_r channel-type address-key]
  (let [ss (parent-of (parent-of _r))
        contacts (get-scape ss (keyword (str (name channel-type) "-address-contact")) true)
        addr (contents _r address-key)
        ca (contents _r :contact-address)
        attribute-type (keyword (str (name channel-type) "-address"))
        ]
    (if (nil? (--> key->resolve _r contacts addr))
      (if ca
        (--> matrice->scape-contact _r ss {:address ca :identifiers {attribute-type addr}})
        (--> matrice->identify _r ss {:identifiers {attribute-type addr} :attributes {:name (str "\"" addr "\"")}})
        ))))
