(ns
  #^{:author "Eric Harris-Braun"
     :doc "Channel receptor"}
  anansi.streamscapes.channel
  (:use [anansi.ceptr]
        [anansi.streamscapes.streamscapes]))

(defmethod manifest :channel [_r name]
           {:name name})
(defmethod state :channel [_r full?]
           (assoc (state-convert _r full?)
             :name (contents _r :name)
             ))
(defmethod restore :channel [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :name (:name state))
             r))
(signal stream receive [_r _f {id :id to :to  envelope :envelope content :content}]
        (rsync _r
               (let [p (parent-of _r)
                     droplet-address (--> matrice->incorporate _r p {:id id :from (address-of _r) :to to :aspect (contents _r :name) :envelope envelope :content content})
                     ]
                 droplet-address)))
