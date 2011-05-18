(ns
  #^{:author "Eric Harris-Braun"
     :doc "Scape receptor"}
  anansi.receptor.scape
  (:use [anansi.ceptr])
  (:use [anansi.map-utilities]))

(defmethod manifest :scape [_r]
           {:map (ref (sorted-map))} )

(defmethod state :scape [_r full?]
           (assoc (state-convert _r full?)
             :map @(contents _r :map)))
(defmethod restore :scape [state parent]
           (let [r (do-restore state parent)]
             (set-content r :map (ref (:map state)))
             r))

;; Signals on the key aspect
(signal key set [_r _f key value]
        (rsync (alter (contents _r :map) assoc key value)))
(signal key resolve [_r _f key]
        (get @(contents _r :map) key))
(signal key all [_r _f]
        (into [] (keys @(contents _r :map))))
(signal key delete [_r _f key]
        (rsync (alter (contents _r :map) dissoc key)))

;; Signals on the address aspect
(signal address resolve [_r _f address]
        (get-keys @(contents _r :map) address))
(signal address all [_r _f]
        (into [] (distinct (vals @(contents _r :map)))))
(signal address delete [_r _f address]
        (rsync (alter (contents _r :map) remove-value address)))

(defn make-scapes
  "instantiate a scape (utility function for building the manifests)"
  [_r manifest & scapes]
  (let [ss (receptor scape _r)
        m  (into manifest (map (fn [s] (let [key (keyword (str (name s) "-scape"))
                                      s (receptor scape _r)]
                                  (--> key->set _r ss key (address-of s))
                                  [key s])) scapes))
        ]
    (assoc m :scapes-scape ss)
    )
  )
