(ns
  #^{:author "Eric Harris-Braun"
     :doc "Scape receptor"}
  anansi.receptor.scape
  (:use [anansi.ceptr])
  (:use [anansi.map-utilities]))

;; Scapes declare dimensions of variability and hold actual related
;; items along those dimensions.  Thus, when creating a scape receptor
;; you define the relationships that it scape.  Currently the typology
;; of the relationships are done by convention in the relationship
;; name: where types are separated by dashes "-" and go from most to
;; least specific.  So:  channel-name specifies that the related item
;; is generally a name (so could go anywhere where names can go) but
;; specifically is a channel name.
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

(signal query all [_r _f]
        (into [] (map (fn [[k v]] [k v]) @(contents _r :map))))

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

(defn rename-scape
  "renames a scape"
  [_r {name :name  new-name :new-name}]
  (if (nil? (_get-scape _r new-name))
    (rsync _r
           (let [ss (get-scape _r :scapes)
                 s (get-scape _r name)
                 s-addr (address-of s)
                 key (scapify new-name)]
             (_delete-content _r (scapify name))
             (_set-content _r key s)
             (--> address->delete _r ss s-addr)
             (--> key->set _r ss key s-addr)))
    (throw (RuntimeException. (str new-name " scape already exists"))))
  )

(defn delete-scape
  "deletes a scape"
  [_r {name :name}]
  (rsync _r
         (let [ss (get-scape _r :scapes)
               s (get-scape _r name)
               s-addr (address-of s)]
           (_delete-content _r (scapify name))
           (--> address->delete _r ss s-addr))))

(defn scape-relationship
  "return the relationship information about the scape"
  [_r aspect]
  (aspect (contents _r :relationship)))

(defn find-scapes-by-relationship [_r rel]
  (find-receptors _r (fn [r] (and (= :anansi.receptor.scape.scape (rdef r :fingerprint)) (or (= (scape-relationship r :key) rel) (= (scape-relationship r :address) rel)))))
  )
  (set! *print-level* 10)

(defn destroy-scape-entries
  "given a scape relationship and a value for that relationship walk through all the scapes that have that relationship and delete the entries for which that relationship matches value"
  [_r relationship value]
  (let [scapes (find-scapes-by-relationship _r relationship)]
    (doseq [s scapes]
      (s-> (if (= relationship (scape-relationship s :key)) key->delete address->delete) s value))))
