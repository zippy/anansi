(ns
  #^{:author "Eric Harris-Braun"
     :doc "Streamscapes receptor"}
  anansi.streamscapes.streamscapes
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.streamscapes.ident])
  (:use [clojure.string :only [join]])
  )

(defmethod manifest :streamscapes [_r matrice-address password data]
           (let [ms (receptor :scape _r)]
             (s-> key->set ms matrice-address :matrice)
             (make-scapes _r  {:password password
                               :matrice-scape ms
                               :data data
                               }
                          :aspect :id :delivery
                          )))

(defmethod state :streamscapes [_r full?]
           (let [base-state (state-convert _r full?)]
             (if full?
               (assoc base-state
                 :password (contents _r :password)
                 :matrice-scape (address-of (get-scape _r :matrice))
                 :data (contents _r :data)
                 )
               (assoc base-state 
                 :data (contents _r :data)
                 :matrices (s-> key->all (get-scape _r :matrice))
                   )))
           )
(defmethod restore :streamscapes [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :password (:password state))
             (restore-content r :matrice-scape (get-receptor r (:matrice-scape state)))
             (restore-content r :data (:data state))
             r))

(defn do-incorporate
  "add a droplet receptor into the streamscape"
  [_r _f {id :id from :from to :to aspect :aspect envelope :envelope content :content}]
  (rsync _r
         (let [d (receptor :droplet _r id from to aspect envelope content)
               addr (address-of d)
               aspects (get-scape _r :aspect)
               ids (get-scape _r :id)
               ]
           (--> key->set _r aspects addr aspect)
           (--> key->set _r ids addr id)
           addr)))

(defn scape-identifier-key [identifier]
  (keyword (str (name identifier) "-ident")))
(defn scape-identifier-attribute-key [identifier]
  (keyword (str "ident-"(name identifier))))

(defn find-identity-by-identifier
  "given an identifier name and identifier value return the identity address if it exists"
  [_r identifier value]
  (let [scape (_get-scape _r (scape-identifier-key identifier))]
    (if (nil? scape) nil (--> key->resolve _r scape value)))
  )

(defn find-identities
  "given a set of identifiers return a lazy seq of all the identities that match those identifiers"
  [_r identifiers]
  (distinct (keep identity (for [[i v] identifiers] (find-identity-by-identifier _r i v))))
  )

(defn do-identify
  "add an identity receptor into the streamscape, scaping the identifiers and attributes appropriately"
  ([_r params] (do-identify _r params true))
  ([_r {identifiers :identifiers attrs :attributes} throw-if-exists]
     (let [attrs1 (if (nil? attrs) {} attrs)
           attributes (if (nil? (:name attrs1))
                        (assoc attrs1 :name (str "name for " (vals identifiers)) )
                        attrs1)
           iaddrs (find-identities _r identifiers)
           iaddr (first iaddrs)
           exists (not (nil? iaddr))]
       (if (and exists throw-if-exists) (throw (RuntimeException. (str "identity already exists for identifiers: " (join ", " (vals identifiers))))))
       (if (not (nil? (first (rest iaddrs))))
         (into [] iaddrs)
         (rsync _r
                (let [ident-address (if exists iaddr (address-of (receptor :ident _r {:name (:name attributes)})))]
                  (doall (for [[i v] identifiers
                                :let [iscape (get-scape _r (scape-identifier-key i) true)]]
                           (--> key->set _r iscape v ident-address)))
                  (doall (for [[a v] attributes
                                :let [iscape (get-scape _r (scape-identifier-attribute-key a) true)]]
                           (--> key->set _r iscape ident-address v)))
                  ident-address))))))

(signal channel incorporate [_r _f params]
        ; TODO add in authentication to make sure that _f is one of this
        ; streamscape instance's channels
        (do-incorporate _r _f params))

(signal matrice incorporate [_r _f params]
        ; TODO add in authentication to make sure that _f is a matrice
        (do-incorporate _r _f params))

(signal matrice identify [_r _f params]
        ; TODO add in authentication to make sure that _f is a matrice
        (do-identify _r params))

(signal matrice make-channel [_r _f params]
        ; TODO add in authentication to make sure that _f is a matrice
        (rsync _r
               (let [cc (receptor :channel _r (:name params))
                     {{in-bridge :bridge  in-params :params} :in
                      {out-bridge :bridge delivery-signal :delivery-signal out-params :params} :out} params
                     in-bridge-address (if in-bridge
                                         (receptor in-bridge cc in-params))
                     out-bridge-address (if out-bridge
                                          (let [b (receptor out-bridge cc out-params)]
                                            (--> key->set cc (get-scape cc :deliverer) :deliverer [(address-of b) delivery-signal])
                                            b))
                     ]
                 (address-of cc))))

