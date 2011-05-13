(ns
  #^{:author "Eric Harris-Braun"
     :doc "Scaping"}
  anansi.scape
  (:use [anansi.map-utilities]
        [anansi.util]
        [anansi.ceptr]))

(defprotocol ScapeStore
  "Protocol for accessing scape data"
  (scape-set [this key address])
  (scape-get [this {:keys [key address]}])
  (scape-delete [this {:keys [key address]}])
  (scape-keys [this])
  (scape-addresses [this])
  (scape-dump [this])
  )

(defmacro make-scape-store [klass]
  `(extend-type ~klass
     ScapeStore
     (~'scape-set [~'this ~'key ~'address] (assoc ~'this ~'key ~'address))
     (~'scape-get [~'this {:keys [~'key ~'address]}]
                (cond
                 (nil? ~'key) (get-keys ~'this ~'address)
                 :else (get ~'this ~'key)))
     (~'scape-delete [~'this {:keys [~'key ~'address]}]
                   (cond
                    (nil? ~'key) (remove-value ~'this ~'address)
                    :else (dissoc ~'this ~'key)))
     (~'scape-keys [~'this] (keys ~'this))
     (~'scape-addresses [~'this] (vals ~'this))
     (~'scape-dump [~'this] {:type "HashScape", :contents (into {} ~'this)})
     ))
(make-scape-store clojure.lang.PersistentArrayMap)
(make-scape-store clojure.lang.PersistentTreeMap)

(defn make-hash-scape
  "create a new HashScape from a hash"
  ([the_hash] the_hash)
  ([] (make-hash-scape {})))

(defn make-scapes-ref
  "create a ref to a hash of empty scapes"
  [& scape-names]
  (ref (zipmap scape-names (repeatedly (count scape-names) #(make-hash-scape)))))

(defn dump-scapes
  "convert scapes into pretty printable hash"
  [scapes]
  (into {} (map (fn [[key value]] [key (scape-dump value)]) scapes))
  )

(defn unserialize-scape 
  "convert a scape dump back into a a scape"
  [dump-hash]
  (let [scape-type (:type dump-hash)
        scape (condp  = scape-type
                  "HashScape" (make-hash-scape (:contents dump-hash)) 
                  (throw (RuntimeException. (str "Unknown scape type: '" scape-type "'"))))]
    scape))

(defn unserialize-scapes
  "convert scapes dump into scapes hash"
  [scapes-dump]
  (into {} (map (fn [[scape-name scape-dump]] [scape-name (unserialize-scape scape-dump)]) scapes-dump))
  )

(defn alter-scape-set
    "Utility function to change a scape (must be called within dosync)"
    [scapes-ref scape-name key address]
    (alter scapes-ref assoc scape-name (scape-set (scape-name @scapes-ref) key address))
    )

(defn alter-scape-unset-key
  "Utility function to remove a scape key (must be called within dosync)"
  [scapes-ref scape-name key]
  (alter scapes-ref assoc scape-name (scape-delete (scape-name @scapes-ref) {:key key}))
  )

(defn alter-scape-unset-address
  "Utility function to remove a scape address (must be called within dosync)"
  [scapes-ref scape-name address]
  (alter scapes-ref assoc scape-name (scape-delete (scape-name @scapes-ref) {:address address}))
  )

(defn alter-scape-change
  "Utility function to change the key of an address"
  [scapes-ref scape-name key address]
  (alter scapes-ref assoc scape-name (assoc (scape-delete (scape-name @scapes-ref) {:address address}) key address)))

(defn calculate-angles
  "calculate angles from the seat scape"
  [seat-scape]
  (let [size (count seat-scape)]
    (if (> size 0)
      (into (sorted-map) (zipmap (range 0 360 (/ 360 size)) (vals seat-scape)))
      (sorted-map))))

(defn angle-scape-from-seat-scape
  "create angle scape from a seat scape"
  [seat-scape]
  (make-hash-scape (calculate-angles seat-scape))
  )

(defn angle-to-coord
  [degrees radius]
  [(int  (* radius (Math/sin (Math/toRadians degrees)))) (int (* -1 radius (Math/cos (Math/toRadians degrees))))]
  )

(defn calculate-coords
  "calculate coordinate scape from the seat scape"
  [seat-scape radius]
  (let [size (count seat-scape)]
    (if (> size 0)
      (into {} (zipmap (map #(angle-to-coord % radius) (range 0 360 (/ 360 size))) (vals seat-scape)))
      (sorted-map))))

(defn calculate-holding-coord
  "cacluate the x,y coordinate of an object held by a person based on the angle scape"
  [to-address angle-scape radius]
  (let [[to-angle] (scape-get angle-scape {:address to-address})
        ]
    (if (nil? to-angle)
      (throw ( RuntimeException. (str to-address " doesn't have an angle in the scape: " angle-scape)))
      (angle-to-coord to-angle (- radius (int (* radius 0.02)))))))

(defn regenerate-holding-coords
  "Recreate the holding coords from the holding and angle scapes"
  [holding-scape angle-scape]
  (into {} (map (fn [[holder-address object-address]] {(calculate-holding-coord holder-address angle-scape 500) object-address}) holding-scape)))

(defn regenerate-coord-scape
  "Recreates the coordinate scape based on the other scapes"
  [coords-scape seat-scape angle-scape holding-scape people-list radius]
  (let [scape-minus-people-and-held-objects (remove-values (remove-values coords-scape people-list) (vals holding-scape))
        people-coords (calculate-coords seat-scape radius)
        holding-coords (regenerate-holding-coords holding-scape angle-scape)
        ]
    (merge scape-minus-people-and-held-objects people-coords holding-coords))
  )

