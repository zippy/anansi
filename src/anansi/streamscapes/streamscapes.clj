(ns
  #^{:author "Eric Harris-Braun"
     :doc "Streamscapes receptor"}
  anansi.streamscapes.streamscapes
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.droplet :only [droplet-def]]
        [anansi.streamscapes.contact :only [contact-def]]
        [anansi.streamscapes.groove :only [grammar-match? compository]]
        )
  (:use [clojure.string :only [join]])
  )
(def streamscapes-def (receptor-def "streamscapes"
                                    (scapes {:name :droplet-channel :relationship {:key "droplet-address" :address "channel-address"}}
                                            {:name :channel :relationship {:key "channel-name" :address "channel-address"}}
                                            {:name :droplet-grooves :relationship {:key "droplet-address" :address "groove-names"}}
                                            {:name :channel-type :relationship {:key "channel-address" :address "channel-type"}}
                                            {:name :id :relationship {:key "droplet-address" :address "streamscapes_channel_address"}}
                                            {:name :delivery :relationship {:key "timestamp" :address "droplet-address"}}
                                            {:name :receipt :relationship {:key "timestamp" :address "droplet-address"}}
                                      )
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
(declare match-grooves)
(defn do-incorporate
  "add a droplet receptor into the streamscape"
  [_r _f {id :id from :from to :to channel :channel envelope :envelope content :content deliver :deliver}]
  (rsync _r
         (let [c-out (if channel
                       (let [c-out-addr (--> key->resolve _r (get-scape _r :channel) channel)]
                         (if (nil? c-out-addr)
                           (throw (RuntimeException. (str "Unknown channel: " channel)))
                           (get-receptor _r c-out-addr)))
                       (if deliver (throw (RuntimeException. "Immediate delivery requested but no channel param was given."))))
               d (make-receptor droplet-def _r id from to channel envelope content)
               addr (address-of d)
               droplet-channels (get-scape _r :droplet-channel)
               ids (get-scape _r :id)
               ]
           (if channel
             (let [matches (match-grooves (--> key->resolve _r (get-scape _r :channel-type) (address-of c-out)) _r addr envelope content)]
               ;; if grooved matched a non-native encoding gotta add
               ;; in the matched data into the droplet
               (set-content d :matched-grooves  (into {} (filter (fn [[_ m]] (map? m)) matches)))
               
               (--> key->set _r droplet-channels addr (address-of c-out))))
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

(defn destroy-droplet [_r daddr]
  (do
    (destroy-scape-entries _r "droplet-address" daddr)
    (destroy-receptor _r daddr)))

(defn scape-contact-key [identifier]
  (keyword (str (name identifier) "-contact")))
(defn scape-contact-attribute-key [attribute]
  (keyword (str "contact-"(name attribute))))

(defn find-contact-by-identifier
  "given an identifier name and identifier value return the contact address if it exists"
  [_r identifier value]
  (let [scape (_get-scape _r (scape-contact-key identifier))]
    (if (nil? scape) nil (--> key->resolve _r scape value)))
  )

(defn find-contacts
  "given a set of identifiers return a lazy seq of all the contacts that match those identifiers"
  [_r identifiers]
  (distinct (keep identity (for [[i v] identifiers] (find-contact-by-identifier _r i v))))
  )

(defn do-scape-contact
  "scape a contact address according to a given a set of identifiers and attributes"
  [_r contact-address identifiers attributes]
  (do (if identifiers (doseq [[i v] identifiers
                              :let [iscape (get-scape _r (scape-contact-key i) {:key (keyword (str (name i) "-identifier")) :address :contact-address})]]
                        (--> key->set _r iscape v contact-address)))
      (if attributes  (doseq [[a v] attributes
                              :let [iscape (get-scape _r (scape-contact-attribute-key a) {:key :contact-address :address (keyword (str (name a) "-attribute"))})]]
                        (--> key->set _r iscape contact-address v))))
  )
(defn- make-default-name [identifiers]
  (str "\"" (first (vals identifiers)) "\"")
  )
(defn do-identify
  "add an identity receptor into the streamscape, scaping the identifiers and attributes appropriately"
  ([_r params] (do-identify _r params true))
  ([_r {identifiers :identifiers attrs :attributes} throw-if-exists]
     (let [iaddrs (find-contacts _r identifiers)
           iaddr (first iaddrs)
           exists (not (nil? iaddr))
           attributes (if exists nil
                          (let [attrs1 (if (nil? attrs) {} attrs)]
                            (if (nil? (:name attrs1))
                              (assoc attrs1 :name (make-default-name identifiers) )
                              attrs1)))
           ]
       
       (if (and exists throw-if-exists) (throw (RuntimeException. (str "contact already exists for identifiers: " (join ", " (vals identifiers))))))
       (if (not (nil? (first (rest iaddrs))))
         (into [] iaddrs)
         (rsync _r
                (let [contact-address (if exists iaddr (address-of (make-receptor contact-def _r {:attributes {:name (:name attributes)}})))]
                  (do-scape-contact _r contact-address identifiers attributes)
                  contact-address))))))

(signal matrice create-contact [_r _f {identifiers :identifiers attrs :attributes override-uniquness-check :override-uniquness-check}]
        ;; TODO add in authentication to make sure that _f is a matrice
        (do
          (if (not override-uniquness-check)
            (let [iaddrs (find-contacts _r identifiers)]
              (if (not (nil? (first iaddrs)))
                (throw (RuntimeException. (str "There are contacts already identified by one or more of: "  (join ", " (vals identifiers)))))
                )))
          (let [attrs1 (if (nil? attrs) {} attrs)
                attributes (if (nil? (:name attrs1))
                             (assoc attrs1 :name (make-default-name identifiers))
                             attrs1)
                contact-address (address-of (make-receptor contact-def _r {:attributes {:name (:name attributes)}}))
                ]
            (do-scape-contact _r contact-address identifiers attributes)
            contact-address
            )))

(defn- get-contact [_r contact-address]
  (let [contact (get-receptor _r contact-address)]
    (if (or (nil? contact) (not= (rdef contact :fingerprint) :anansi.streamscapes.contact.contact)) (throw (RuntimeException. (str "No such contact: "  contact-address))))
    contact
    )
  )

(signal matrice scape-contact [_r _f {contact-address :address identifiers :identifiers attributes :attributes}]
         ;; TODO add in authentication to make sure that _f is one of this
         ;; streamscape instance's channels
         (let [contact (get-contact _r contact-address)]
           (do-scape-contact _r contact-address identifiers attributes)))

(signal matrice delete-contact [_r _f {contact-address :address}]
        (let [contact (get-contact _r contact-address)]
          (rsync _r
                 (destroy-receptor _r contact-address)
                 (destroy-scape-entries _r :contact-address contact-address)
                 )))

(signal channel incorporate [_r _f params]
        ; TODO add in authentication to make sure that _f is one of this
        ; streamscape instance's channels
        (do-incorporate _r _f params))

(signal matrice incorporate [_r _f params]
        ; TODO add in authentication to make sure that _f is a matrice
        (do-incorporate _r _f params))

(signal matrice discorporate [_r _f {daddr :droplet-address}]
        (if (s-> key->resolve (get-scape _r :id) daddr)
          (rsync _r
               (destroy-droplet _r daddr)
               nil)
          (throw (RuntimeException. (str "no such droplet: " daddr)))))

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
  (let [cc (first (find-receptors _r (fn [r] (and (= :anansi.streamscapes.channel.channel (rdef r :fingerprint)) (= (contents r :name) name)))))]
    (if (nil? cc)
      (throw (RuntimeException. (str "channel not found: " name))))
    cc))

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
(signal setup update-channel [_r _f params]
        (rsync _r
               (let [channel-address (:channel-address params)
                     channel (get-receptor _r channel-address)
                     type (--> key->resolve _r (get-scape _r :channel-type) channel-address)
                     channel-scape (get-scape _r :channel)
                     [current-name] (--> address->resolve _r channel-scape channel-address)
                     new-name (:name params)
                     ]
                 (if (not= current-name new-name)
                   (do
                     (if (not (nil? (--> key->resolve _r channel-scape new-name)))
                       (throw (RuntimeException. (str "channel name '" (name new-name) "' already exists" ))))
                     (--> key->set _r channel-scape new-name channel-address)
                     (--> key->delete _r channel-scape current-name)))
                 (--> (get-signal-function "anansi.streamscapes.channel" "setup" "update-by-type") _r channel type params)

                 ))
        )
(signal setup new-channel [_r _f params]
        (let [{raw-type :type n :name} params
              type (keyword (name raw-type))
              channel-address (s-> matrice->make-channel _r {:name (channel-name n)
                                             :receptors
                                             (condp = type
                                                 :streamscapes {(get-receptor-definition :anansi.streamscapes.channels.local-bridge-in.local-bridge-in)
                                                                {:role :receiver :params {}
                                                                 :signal ["anansi.streamscapes.channels.local-bridge-in" "cheat" "receive"]}
                                                                (get-receptor-definition :anansi.streamscapes.channels.local-bridge-out.local-bridge-out)
                                                                {:role :deliverer :params {}
                                                                 :signal ["anansi.streamscapes.channels.local-bridge-out" "channel" "deliver"]}}
                                                 :twitter (let [{search-query :search-query} params]
                                                            {(get-receptor-definition :anansi.streamscapes.channels.twitter-bridge-in.twitter-bridge-in)
                                                             {:role :receiver :params {}}
                                                             (get-receptor-definition :anansi.streamscapes.channels.twitter-controller.twitter-controller)
                                                             {:role :controller :params {:attributes {:search-query search-query}}
                                                              :signal ["anansi.streamscapes.channels.twitter-controller" "channel" "control"]}})
                                                 :irc (let [{host :host port :port user :user nick :nick contact-address :contact-address} params]
                                                        {(get-receptor-definition :anansi.streamscapes.channels.irc-bridge-in.irc-bridge-in)
                                                         {:role :receiver :params {} }
                                                         (get-receptor-definition :anansi.streamscapes.channels.irc-bridge-out.irc-bridge-out)
                                                         {:role :deliverer
                                                          :params {}
                                                          :signal ["anansi.streamscapes.channels.irc-bridge-out" "channel" "deliver"] }
                                                         (get-receptor-definition :anansi.streamscapes.channels.irc-controller.irc-controller)
                                                         {:role :controller
                                                          :signal ["anansi.streamscapes.channels.irc-controller" "channel" "control"]
                                                          :params {:attributes {:host host :port port :user user :nick nick :contact-address contact-address}}}})
                                                 :xmpp (let [{host :host domain :domain username :username pass :password contact-address :contact-address} params]
                                                        {(get-receptor-definition :anansi.streamscapes.channels.xmpp-bridge-in.xmpp-bridge-in)
                                                         {:role :receiver :params {} }
                                                         (get-receptor-definition :anansi.streamscapes.channels.xmpp-bridge-out.xmpp-bridge-out)
                                                         {:role :deliverer
                                                          :params {}
                                                          :signal ["anansi.streamscapes.channels.xmpp-bridge-out" "channel" "deliver"] }
                                                         (get-receptor-definition :anansi.streamscapes.channels.xmpp-controller.xmpp-controller)
                                                         {:role :controller
                                                          :signal ["anansi.streamscapes.channels.xmpp-controller" "channel" "control"]
                                                          :params {:attributes {:host host :domain domain :username username :_password pass :contact-address contact-address}}}})
                                                 :email (let [{in :in out :out} params
                                                              r1 (if (not (nil? in))
                                                                   {(get-receptor-definition :anansi.streamscapes.channels.email-bridge-in.email-bridge-in)
                                                                    {:role :receiver :params {:attributes in} }
                                                                    (get-receptor-definition :anansi.streamscapes.channels.email-controller.email-controller)
                                                                    {:role :controller
                                                                     :signal ["anansi.streamscapes.channels.email-controller" "channel" "control"]}
                                                                    }
                                                                   {}
                                                                   )
                                                              r2 (if (not (nil? out))
                                                                   (merge
                                                                    r1
                                                                    {(get-receptor-definition :anansi.streamscapes.channels.email-bridge-out.email-bridge-out)
                                                                     {:role :deliverer
                                                                      :signal ["anansi.streamscapes.channels.email-bridge-out" "channel" "deliver"]
                                                                      :params {:attributes out}}})
                                                                   r1
                                                                   )]
                                                          r2)
                                                 (throw (RuntimeException. (str "channel type '" (name type) "' not implemented" ))))})]
          (--> key->set _r (get-scape _r :channel-type) channel-address type)
          channel-address))

(signal matrice control-channel [_r _f {n :name cmd :command params :params}]
        ;; TODO should be doing a check on the from here ...
        (let [cc (find-channel-by-name _r (channel-name n))]
          (--> (get-signal-function "anansi.streamscapes.channel" "stream" "control") _r cc  {:command (keyword cmd) :params params})
          ))

(signal setup delete-channel [_r _f {n :name}]
        ;; TODO should be doing a check on the from here ...
        (rsync _r
               (let [cc (find-channel-by-name _r (channel-name n))
                     caddr (address-of cc)
                     droplet-channel (get-scape _r :droplet-channel)]
                 (doseq [daddr (--> address->resolve _r droplet-channel caddr)]
                   (destroy-droplet _r daddr))                 
                 (destroy-scape-entries _r "channel-address" caddr)
                 (destroy-receptor _r caddr)
                 nil))
        )

(signal setup new-scape [_r _f params]
        ;; TODO should be doing a check on the from here ...
        (address-of (add-scape _r params)))

(signal setup rename-scape [_r _f params]
        ;; TODO should be doing a check on the from here ...
        (rename-scape _r params)
        )

(signal setup delete-scape [_r _f params]
        ;; TODO should be doing a check on the from here ...
        (delete-scape _r params)
        )

(signal scape set [_r _f {name :name key :key address :address}]
        ;; TODO should be doing a check on the from here ...
        (let [scape (get-scape _r name)]
          (--> key->set _r scape key address)
          nil
          ))

(signal scape delete [_r _f {name :name key :key}]
        ;; TODO should be doing a check on the from here ...
        (let [scape (get-scape _r name)]
          (--> key->delete _r scape key)
          nil
          ))

(defn match-grooves
  "run through the defined grooves and create scape entries for all grooves that match this droplet"
  [channel-type ss droplet-address envelope content]
  (let [host (parent-of ss)
        grooves (get-scape host :groove)
        all (s-> query->all grooves)
        raw-matches (map (fn [[groove-name groove-address]]
                           (let [groove (get-receptor host groove-address)
                                 carriers (contents groove :carriers)]
                             (if (contains? carriers channel-type)
                               (let [carrier (channel-type carriers)
                                     encoding (:encoding carrier)
                                     grammar (cond (nil? encoding) (contents groove :grammar)
                                                   (keyword? encoding) (-> compository groove-name :matchers encoding))]
                                 (try
                                   (let [match (grammar-match? grammar envelope content)]
                                     (if match [groove-name match] nil))
                                   (catch Exception e
                                     (println (str e)))
                                   )))))
                         all)
        matched-grooves (into [] (keep identity raw-matches))
        ]
    (doseq [[groove-name match] matched-grooves]
      (let [scape-name (keyword (str (name groove-name) "-groove"))
            groove-scape (get-scape ss scape-name {:key "droplet-address" :address "boolean"})]
        (s-> key->set groove-scape droplet-address true))
      )
    (let [dg-scape (get-scape ss :droplet-grooves)]
      (s-> key->set dg-scape droplet-address (map (fn [[ g m]] g) matched-grooves)))
    matched-grooves))

