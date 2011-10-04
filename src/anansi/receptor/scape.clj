(ns
  #^{:author "Eric Harris-Braun"
     :doc "Scape receptor"}
  anansi.receptor.scape
  (:use [anansi.ceptr])
  (:use [anansi.map-utilities]))

(def scape-def
     (receptor-def "scape"
                   (manifest [_r & [key addr]]
                             {:map (ref (sorted-map))
                              :relationship {:key key :address addr}}
                             )
                   (state [_r full?]
                          (assoc (state-convert _r full?)
                            :map @(contents _r :map)
                            :relationship (contents _r :relationship))
                          )
                   (restore [state parent r-def]
                            (let [r (do-restore state parent r-def)]
                              (restore-content r :map (ref (:map state)))
                              (restore-content r :relationship (:relationship state))
                              r))
                   ))

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
           (add-scape receptor (if (map? create-if-non-existent) {:name scape-name :relationship create-if-non-existent} scape-name))
           (throw (RuntimeException. (str scape-name " scape doesn't exist"))))
         scape))))

(defn- build-scape-def [d]
  (cond (map? d) {:name (scapify (:name d)) :relationship (:relationship d)}
        (keyword? d) {:name (scapify d)}
        (string? d) {:name (keyword (scapify d))}
        true (throw (RuntimeException. (str "Can't understand '" d "' as a scape definition")))))

(defn make-scapes
  "instantiate a scape (utility function for building the manifests)"
  [_r manifest & scapes]
  (let [ss (make-receptor scape-def _r :scape-name :address)
        m  (into manifest (map (fn [def] (let [{scape-name :name {key-rel :key addr-rel :address} :relationship} (build-scape-def def)
                                            s (make-receptor scape-def _r key-rel addr-rel)]
                                  (--> key->set _r ss scape-name (address-of s))
                                  [scape-name s])) scapes))
        ]
    (assoc m :scapes-scape ss)
    )
  )

(defn add-scape
  "add a new scape into a receptor"
  [_r params]
  (let [{scape-name :name {key-rel :key addr-rel :address} :relationship} (if (map? params) params {:name params})]
    (if (nil? (_get-scape _r scape-name))
      (rsync _r
             (let [s (make-receptor scape-def _r key-rel addr-rel)
                   key (scapify scape-name)
                   ss (get-scape _r :scapes)]
               (--> key->set _r ss key (address-of s))
               (_set-content _r key s)
               s))
      (throw (RuntimeException. (str scape-name " scape already exists"))))))

(defn scape-relationship
  "return the relationship information about the scape"
  [_r aspect]
  (aspect (contents _r :relationship)))

(defn query-scape
  "returns a lazy sequence of the results of the qfun filtered against the scape values
qfun must be a function of two arguments: key, address and must return a vector pair of a boolean value of weather to include an entry for this pair, plus the value to be returned for this pair, which may anything."
  [_r qfun]
  (map (fn [[_ result ]] result) (filter (fn [[b r]] b) (map (fn [[k a]] (qfun k a) ) @(contents _r :map)))))

(defn sort-by-scape
  "takes a list of receptor addresses and returns them in order sorted by the scape key
assumes that the scape has receptor addresses in the value of the map"
  [_r addresses]
  (let [a (set addresses)
        m (filter (fn [[k v]] (a v)) @(contents _r :map)) ;scape pairs in receptor list 
        ]
    (into [] (map (fn [[k v]] v) (sort-by (fn [[k v]] k) m)))
    )
  )
