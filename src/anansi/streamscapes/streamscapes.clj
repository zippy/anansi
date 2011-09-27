(ns
  #^{:author "Eric Harris-Braun"
     :doc "Streamscapes receptor"}
  anansi.streamscapes.streamscapes
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.droplet :only [droplet-def]]
        [anansi.streamscapes.ident :only [ident-def]]
        )
  (:use [clojure.string :only [join]])
  )
(def streamscapes-def (receptor-def "streamscapes"
                       (scapes {:name :aspect :relationship {:key :address :address :streamscapes-aspect}}
                               {:name :id :relationship {:key :address :address :streamscapes-channel-address}}
                               {:name :delivery :relationship {:key :streamscapes-aspect-time-map :address :address}})
                       (attributes :data :_password)
                       (manifest [_r {matrice-address :matrice-addr attrs :attributes}]
                                 (let [ms (make-receptor scape-def _r)]
                                   (s-> key->set ms matrice-address :matrice)
                                   (apply make-scapes _r
                                          (merge (extract-receptor-attributes-from-map _r attrs)
                                                 {:matrice-scape ms})
                                          (rdef _r :scapes))))
                       (state [_r full?]
                              (let [base-state (state-convert _r full?)]
                                (if full?
                                  (assoc base-state :matrice-scape (address-of (get-scape _r :matrice)))
                                  (assoc base-state :matrices (s-> key->all (get-scape _r :matrice))))))
                       (restore [state parent r-def]                                
                                (let [r (do-restore state parent r-def)]
                                  (doall (map (fn [a] (restore-content r a (a state))) (:attributes r-def)))
                                  (restore-content r :matrice-scape (get-receptor r (:matrice-scape state)))
                                  r)
                                )))

(defn do-incorporate
  "add a droplet receptor into the streamscape"
  [_r _f {id :id from :from to :to aspect :aspect envelope :envelope content :content}]
  (rsync _r
         (let [d (make-receptor droplet-def _r id from to aspect envelope content)
               addr (address-of d)
               aspects (get-scape _r :aspect)
               ids (get-scape _r :id)
               ]
           (--> key->set _r aspects addr aspect)
           (--> key->set _r ids addr (contents d :id)) 
                                        ; don't use the id from the
                                        ; params because it may have been nil in which
                                        ; case the instantiation code will have created the id on the fly
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
                (let [ident-address (if exists iaddr (address-of (make-receptor ident-def _r {:attributes {:name (:name attributes)}})))]
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
               (let [cc (make-receptor (get-receptor-definition :anansi.streamscapes.channel.channel) _r {:attributes {:name (:name params)}})]
                 (doall
                  (map (fn [[r-def p]]
                               (let [{receptor-role :role signal :signal receptor-params :params} p
                                     r (make-receptor r-def cc receptor-params)]
                                 (--> key->set cc (get-scape cc receptor-role) receptor-role [(address-of r) signal])))
                             (:receptors params)))
                 (address-of cc))))

(defn find-channel-by-name [_r name]
  (first (find-receptors _r (fn [r] (and (= :anansi.streamscapes.channel.channel (rdef r :fingerprint)) (= (contents r :name) name)))))
  )

(signal streamscapes receive [_r _f message]
        (let [name (:aspect message)
              cc (find-channel-by-name _r name)
              ]
          (if (nil? cc)
            (throw (RuntimeException. (str "channel not found: " name))))
          (--> (get-signal-function "anansi.streamscapes.channel" "bridge" "receive") _r cc message)
          ))

(signal setup new-channel [_r _f {raw-type :type stream-name :name host :host port :port user :user nick :nick}]
        (let [type (keyword (name raw-type))]
          (s-> matrice->make-channel _r {:name (keyword (str (name type) "-" (name stream-name) "-stream"))
                                         :receptors
                                         (condp = type
                                             :irc {(get-receptor-definition :anansi.streamscapes.channels.irc-bridge-in.irc-bridge-in) {:role :receiver :params {} }
                                                   (get-receptor-definition :anansi.streamscapes.channels.irc-controller.irc-controller) {:role :controller :signal (get-signal-function "anansi.streamscapes.channels.irc-controller" "channel" "control") :params {:attributes {:host host :port port :user user :nick nick}}}}
                                             (throw (RuntimeException. (str "channel type '" (name type) "' not implemented" ))))})))
