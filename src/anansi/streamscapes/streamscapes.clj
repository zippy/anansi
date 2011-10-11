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
                                    (scapes {:name :droplet-channel :relationship {:key :droplet-address :address :streamscapes-channel}}
                                            {:name :channel :relationship {:key :name :address :address}}
                                            {:name :id :relationship {:key :droplet-address :address :streamscapes-channel-address}}
                                            {:name :delivery :relationship {:key :timestamp :address :droplet-address}}
                                            {:name :receipt :relationship {:key :timestamp :address :droplet-address}})
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
  [_r _f {id :id from :from to :to channel :channel envelope :envelope content :content deliver :deliver}]
  (rsync _r
         (let [c-out (if channel
                       (let [c-out-addr (--> key->resolve _r (get-scape _r :channel) channel)]
                         (if (nil? c-out-addr)
                           (throw (RuntimeException. (str "Unknown channel: " channel)))
                           (get-receptor _r c-out-addr))))
               d (make-receptor droplet-def _r id from to channel envelope content)
               addr (address-of d)
               droplet-channels (get-scape _r :droplet-channel)
               ids (get-scape _r :id)
               ]
           (if channel
             (--> key->set _r droplet-channels addr channel))
           ;; don't use the id from the params because it may have been nil in which
           ;; case the instantiation code will have created the id on the fly
           (--> key->set _r ids addr (contents d :id))
           (if deliver
             (let [errs (s-> (get-signal-function "anansi.streamscapes.channel" "stream" "send") c-out {:droplet-address addr})]
               ;; TODO: should we throw here?  Probably we need to
               ;; change return result to have this extra info, not
               ;; just the address.
               ))
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
                                :let [iscape (get-scape _r (scape-identifier-key i) {:key (keyword (str (name i) "-identifier")) :address :ident-address})]]
                           (--> key->set _r iscape v ident-address)))
                  (doall (for [[a v] attributes
                               :let [iscape (get-scape _r (scape-identifier-attribute-key a) {:key :ident-address :address (keyword (str (name a) "-attribute"))})]]
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
               (let [n (:name params)
                     cc (make-receptor (get-receptor-definition :anansi.streamscapes.channel.channel) _r {:attributes {:name n}})
                     cc-addr (address-of cc)]
                 (--> key->set _r (get-scape _r :channel) n cc-addr)
                 (doall
                  (map (fn [[r-def p]]
                               (let [{receptor-role :role signal :signal receptor-params :params} p
                                     r (make-receptor r-def cc receptor-params)]
                                 (--> key->set cc (get-scape cc receptor-role) receptor-role [(address-of r) signal])))
                             (:receptors params)))
                 cc-addr)))

(defn find-channel-by-name [_r name]
  (first (find-receptors _r (fn [r] (and (= :anansi.streamscapes.channel.channel (rdef r :fingerprint)) (= (contents r :name) name)))))
  )

(signal streamscapes receive [_r _f message]
        (let [name (:channel message)
              cc (find-channel-by-name _r name)
              ]
          (if (nil? cc)
            (throw (RuntimeException. (str "channel not found: " name))))
          (--> (get-signal-function "anansi.streamscapes.channel" "bridge" "receive") _r cc message)
          ))

(defn channel-name [n]
                                        ;  (keyword (str (name type) "-" (name n) "-stream"))
  n
  )
(signal setup new-channel [_r _f params]
        (let [{raw-type :type n :name} params
              type (keyword (name raw-type))]
          (s-> matrice->make-channel _r {:name (channel-name n)
                                         :receptors
                                         (condp = type
                                             :streamscapes {(get-receptor-definition :anansi.streamscapes.channels.local-bridge-in.local-bridge-in)
                                                            {:role :receiver :params {}
                                                             :signal (get-signal-function "anansi.streamscapes.channels.local-bridge-in" "cheat" "receive")}
                                                            (get-receptor-definition :anansi.streamscapes.channels.local-bridge-out.local-bridge-out)
                                                            {:role :deliverer :params {}
                                                             :signal (get-signal-function "anansi.streamscapes.channels.local-bridge-out" "channel" "deliver")}}
                                             :twitter (let [{search-query :search-query} params]
                                                        {(get-receptor-definition :anansi.streamscapes.channels.twitter-bridge-in.twitter-bridge-in)
                                                         {:role :receiver :params {}}
                                                         (get-receptor-definition :anansi.streamscapes.channels.twitter-controller.twitter-controller)
                                                         {:role :controller :params {:attributes {:search-query search-query}}
                                                          :signal (get-signal-function "anansi.streamscapes.channels.twitter-controller" "channel" "control")}})
                                             :irc (let [{host :host port :port user :user nick :nick} params]
                                                    {(get-receptor-definition :anansi.streamscapes.channels.irc-bridge-in.irc-bridge-in)
                                                     {:role :receiver :params {} }
                                                     (get-receptor-definition :anansi.streamscapes.channels.irc-controller.irc-controller)
                                                     {:role :controller
                                                      :signal (get-signal-function "anansi.streamscapes.channels.irc-controller" "channel" "control")
                                                      :params {:attributes {:host host :port port :user user :nick nick}}}})
                                             :email (let [{in :in out :out} params
                                                          r1 (if (not (nil? in))
                                                               {(get-receptor-definition :anansi.streamscapes.channels.email-bridge-in.email-bridge-in)
                                                                {:role :receiver :params {:attributes in} }
                                                                (get-receptor-definition :anansi.streamscapes.channels.email-controller.email-controller)
                                                                {:role :controller
                                                                 :signal (get-signal-function "anansi.streamscapes.channels.email-controller" "channel" "control")}
                                                                }
                                                               {}
                                                               )
                                                          r2 (if (not (nil? out))
                                                               (merge
                                                                r1
                                                                {(get-receptor-definition :anansi.streamscapes.channels.email-bridge-out.email-bridge-out)
                                                                 {:role :deliverer
                                                                  :signal (get-signal-function "anansi.streamscapes.channels.email-bridge-out" "channel" "deliver")
                                                                  :params {:attributes out}}})
                                                               r1
                                                               )]
                                                      r2)
                                             (throw (RuntimeException. (str "channel type '" (name type) "' not implemented" ))))})))

(signal matrice control-channel [_r _f {n :name cmd :command params :params}]
        ;; TODO should be doing a check on the from here ...
        (let [cc (find-channel-by-name _r (channel-name n))]
          (if (nil? cc)
            (throw (RuntimeException. (str "channel not found: " n))))
          (--> (get-signal-function "anansi.streamscapes.channel" "stream" "control") _r cc  {:command (keyword cmd) :params params})
          ))
