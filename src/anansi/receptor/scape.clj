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
             (restore-content r :map (ref (:map state)))
             r))

;; Signals on the key aspect
(signal key set [_r _f key value]
        (rsync _r (alter (contents _r :map) assoc key value)))
(signal key resolve [_r _f key]
        (get @(contents _r :map) key))
(signal key all [_r _f]
        (into [] (keys @(contents _r :map))))
(signal key delete [_r _f key]
        (rsync _r (alter (contents _r :map) dissoc key)))

;; Signals on the address aspect
(signal address resolve [_r _f address]
        (get-keys @(contents _r :map) address))
(signal address all [_r _f]
        (into [] (distinct (vals @(contents _r :map)))))
(signal address delete [_r _f address]
        (rsync _r (alter (contents _r :map) remove-value address)))

(declare add-scape)
(defn get-scape
  "return the named scape receptor"
  ([receptor scape-name]
     (get-scape receptor scape-name false))
  ([receptor scape-name create-if-non-existent]
     (let [scape (_get-scape receptor scape-name)]
       (if (nil? scape)
         (if create-if-non-existent
           (add-scape receptor scape-name)
           (throw (RuntimeException. (str scape-name " scape doesn't exist"))))
         scape))))

(defn make-scapes
  "instantiate a scape (utility function for building the manifests)"
  [_r manifest & scapes]
  (let [ss (receptor :scape _r)
        m  (into manifest (map (fn [s] (let [key (scapify s)
                                      s (receptor :scape _r)]
                                  (--> key->set _r ss key (address-of s))
                                  [key s])) scapes))
        ]
    (assoc m :scapes-scape ss)
    )
  )

(defn add-scape
  "add a new scape into a receptor"
  [_r scape-name]
  (if (nil? (_get-scape _r scape-name))
    (rsync _r
           (let [s (receptor :scape _r)
                 key (scapify scape-name)
                 ss (get-scape _r :scapes)]
             (--> key->set _r ss key (address-of s))
             (_set-content _r key s)
             s))
    (throw (RuntimeException. (str scape-name " scape already exists")))))
