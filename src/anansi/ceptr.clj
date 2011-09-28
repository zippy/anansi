(ns
  #^{:author "Eric Harris-Braun"
     :doc "basic ceptr functions"}
  anansi.ceptr
  (:use [anansi.map-utilities]
        [anansi.util]
        ))

(declare *receptors*)

(def *changes* (ref 0))

(defmacro rsync [r & body]
  `(dosync (alter *changes* + 1) (if (not (nil? ~r)) (alter ~r assoc :changes (+ 1 (:changes @~r)))) ~@body))

(def *signals* (ref {}))
(defn get-signal [s] {s @*signals*})

(def *definitions* (ref {}))
(defn get-receptor-definition [fingerprint]
  (let [d (fingerprint @*definitions*)]
    (if (nil? d) (throw (RuntimeException. (str "No receptor found for fingerprint: " fingerprint " Known fingerprints are:" (keys @*definitions*)))))
    d
    ))

(defmacro signal [aspect name args & body]
  "Creates a signal function on an aspect"
  `(do
     (let [~'signal-function (defn ~(symbol (str aspect "->" name)) ~args ~@body)]
       (dosync (alter *signals* assoc (keyword (str (ns-name *ns*) "." '~aspect "." '~name)) {:fn ~'signal-function :args '~args})))))

(defn get-signal-function [namespace aspect name]
  (:fn ((keyword (str namespace "." aspect "." name)) @*signals*)))

(defmulti manifest (fn [receptor & args] (:type @receptor)))
(defmethod manifest :default [receptor  & args] {})

(defmulti state (fn [receptor full?] (:type @receptor)))
(defmulti restore (fn [s p] (:type s)))

(defmulti animate (fn [receptor] (:type @receptor)))
(defmethod animate :default [receptor] receptor)

(defn receptors-container [receptor] (if (nil? receptor) *receptors* (:receptors @receptor)))

(defn contents
  "get an item out of the manifest"
  [receptor key] (key @(:contents @receptor)))

(defn _set-content
  [receptor key value]
  (alter (:contents @receptor) assoc key value))

(defn set-content
  "set the value of a manifest item"
  [receptor key value] (rsync receptor (_set-content receptor key value)))

(defn restore-content
  "set the value of a manifest item without updating the changes count"
  [receptor key value] (dosync (_set-content receptor key value)))

(defmacro receptor [name parent & args]
  "Instantiates a receptor"
  `(let [~'ns-str :last-address ;;(keyword (str (ns-name *ns*) "." '~name))
         ~'receptors (receptors-container ~parent)
         ~'c (~'ns-str @~'receptors)
         ~'addr (if (nil? ~'c) 1 (+ ~'c 1))
         ~'type (keyword ~name)
         ~'r (ref {:type ~'type
                   :parent ~parent,
                   :receptors (ref {}),
                   :address ~'addr
                   :changes 0})
         ]
     (rsync ~parent
            (alter ~'receptors assoc ~'ns-str ~'addr)
            (alter ~'receptors assoc ~'addr ~'r)
            (alter ~'r assoc :contents (ref (manifest ~'r ~@args)))
            (animate ~'r)
            ~'r
            )))

(defn rdef [_r part]
  "returns the definition of the specified part of the receptor"
  (part (:definition @_r)))

(defn make-receptor [definition parent & args]
  "Instantiates a receptor of a given defintion inside the parent"
  (let [ ns-str :last-address ;;(keyword (str (ns-name *ns*) "."
        ;;'~name))
        receptors (receptors-container parent)
        c (ns-str @receptors)
        addr (if (nil? c) 1 (+ c 1))
        r (ref {:definition definition
                :parent parent,
                :receptors (ref {}),
                :address addr
                :changes 0})]
         
       (rsync parent
              (alter receptors assoc ns-str addr)
              (alter receptors assoc addr r)
              (alter r assoc :contents (ref (apply (:manifest definition) r args)))
              ((:animate definition) r)
              r
              )))
(defn extract-receptor-attributes-from-map
  "Utility funciton to extract all the key-value pairs from a map that match a receptors attribute list
   useful for manifest functions who want to pass those values onto make-scape"
  [_r m]
  (into {} (map (fn [a] [a (a m)]) (rdef _r :attributes))))

(defn extract-attribute-values-from-receptor
     [_r full?]
     (let [attrs_ (rdef _r :attributes)
           attrs (if full? attrs_ (filter #(not= \_ (first (name %))) attrs_))]
       (into {} (map (fn [a] [a (contents _r a)]) attrs)))
     )

(def base-manifest `(fn [~'r {~'attrs :attributes}]
                                      (apply ~'anansi.receptor.scape/make-scapes
                                             ~'r
                                             (extract-receptor-attributes-from-map ~'r ~'attrs)
                                             (rdef ~'r :scapes))))
(def form-map {'attributes {:validate (fn [args] (if (< (count args) 1) (throw (RuntimeException. "attributes receptor form requires a list of attributes")) true ))
                            :default `()}
               'scapes {:default `()}
               'manifest {:default base-manifest}
               'animate {:default `(fn [~'r] ~'r)}
               'state {:default `(fn [~'r ~'full?]
                                   (merge (state-convert ~'r ~'full?)
                                          (extract-attribute-values-from-receptor ~'r ~'full?))
                                   )}
               'restore {:default `(fn [~'state ~'parent ~'r-def]
                                     (let [~'r (do-restore ~'state ~'parent ~'r-def)]
                                       (doall (map (fn [~'a] (restore-content ~'r ~'a (~'a ~'state))) (:attributes ~'r-def)))
                                       ~'r))}})

(defn merge-forms [& forms]
  (merge form-map (into {} (map (fn [[form-name & args]] (if (contains? form-map form-name)
                                                          [form-name (assoc (form-name form-map) :args args) ]
                                                          (throw (RuntimeException. (str "unknown receptor form '" form-name "'"))))) forms))))
(defn validate-forms [map]
  (doseq [[form-name def] map]
    (if (and (contains? def :args) (contains? def :validate))
      ((:validate def) (:args def)) )))

(defmacro receptor-def
  "Macro to create a receptor definition.
   Definitons include the following forms which take simple lists:
   attribute, scapes
   And following forms which take function defintions (i.e an argument binding form and a body):
   manifest, animate, state and restore.

   Additionally this macro records the definition the *definitions* var so that it can be searched
   for by fingerprint, which will be of the form <clojure namespace>.<rname>"
  [rname & forms]
  (let [forms-given (apply merge-forms forms)]
    (if-not (string? rname) (throw (RuntimeException. (str  "Expecting first argument to be a string as the definition name.  Instead got: " rname))))
    (validate-forms forms-given)
    (let [fp (keyword (str (ns-name *ns*) "." rname))
          definition
          (into {:fingerprint fp}
                (map (fn [[form-name def]]
                       (let [args (:args def)
                             n (keyword form-name)
                                        ;an args part will have been
                                        ;added by merge-forms if the
                                        ;form was defined, and thus we
                                        ;shouldn't use the default
                             part (if (contains? def :args)
                                    (if (form-name #{'attributes 'scapes})
                                      `#{~@args}
                                      `(fn ~@args))
                                    (:default def)
                                    )]
;                         (prn "XXX:" form-name "--" part)
                         `[~n ~part])
                       ) forms-given))
          ]
      ` (dosync
         (let [~'d ~definition]
           (alter *definitions* assoc ~fp ~'d)
           ~'d))
        )))

(defn parent-of
  "return the receptor that is a receptor's parent"
  [receptor] (:parent @receptor))

(defn get-receptor
  "get a contained receptor by address"
  [receptor address]
  (let [receptors @(receptors-container receptor)]
    (get receptors address)))

(defn address-of
  "get the address of a receptor"
  [receptor] (:address @receptor))

(defn -->
  "send a signal to a receptor"
  [signal from-receptor to & args]
  (let [to-receptor (if (instance? clojure.lang.Ref to) to (get-receptor (parent-of from-receptor) to))]
    (apply signal to-receptor  (address-of from-receptor) args)))

(defn s->
  "send a signal to yourself"
  [signal receptor & args]
  (apply --> signal receptor receptor args)
  )

(defn p->
  "send a signal from the parent"
  [signal receptor & args]
  (apply --> signal nil receptor args)
  )

(defn destroy-receptor
  "destroy a contained receptor by address"
  [receptor address]
  (rsync receptor (alter (receptors-container receptor) dissoc address)))

(defn scapify [scape-name]
  (keyword (str (name scape-name) "-scape")))

(defn scape-state [_r scape-name]
  "return the public (not full?) scape-state as a map"
  (let [s (contents _r scape-name)]
    {:values @(contents s :map) :relationship (contents s :relationship)}))

(defn _get-scape [receptor scape-name]
  (contents receptor (scapify scape-name))
  )

(declare receptor-state)
(defn state-convert
  "worker function to serialize the standard contents of a recptor"
  [receptor full?]
  (let [r @receptor
        rc @(:receptors r)
        s1 {:fingerprint (rdef receptor :fingerprint)
            :address (:address r)
            :changes (:changes r)
            }
        s (if (empty? rc) s1
              (assoc s1
                :receptors (assoc (modify-vals (fn [x] (receptor-state x full?))  (filter (fn [[k v]] (and (not= k :last-address) (or full? (not= (rdef v :fingerprint) :anansi.receptor.scape.scape)) )) rc))
                             :last-address (:last-address rc))))
        ss (_get-scape receptor :scapes)]
    (if (and (not full?) ss)
      (let [scapes (keys @(contents ss :map))] ;; this is cheating 
        (assoc s :scapes (into {} (map (fn [sn] [sn (scape-state receptor sn)] ) scapes))))
      (if ss (assoc s :scapes-scape-addr (address-of ss)) s)
      )))
(defn receptor-state [receptor full?] ((rdef receptor :state) receptor full?))

(defmethod state :default [receptor full?]
           (state-convert receptor full?))

(declare receptor-restore)
(defn do-restore [state parent r-def]
  (let [r (ref {})
        rc (:receptors state)
        sr (modify-vals (fn [s] (receptor-restore s r)) (filter (fn [[k v]] (not= k :last-address)) rc))
        sr1 (if (:last-address rc) (assoc sr :last-address (:last-address rc)) sr)
        c {}
        r1 {:definition r-def
            :address (:address state)
            :receptors (ref sr1)
            :parent parent
            :changes (:changes state)
            :contents (ref c)
            }
        ]
    (dosync (alter r merge r1)
            (let [ss-addr (:scapes-scape-addr state)]
              (if ss-addr (let [ss (get-receptor r ss-addr)]
                            (doseq [[k v]  @(contents ss :map)] (restore-content r k (get-receptor r v))) 
                            (restore-content r :scapes-scape ss)))))
    ((:animate r-def) r)
    r)
  )

(defmethod restore :default [state parent] (do-restore state parent nil) )
(defn receptor-restore [state parent]
  (let [r-def (get-receptor-definition (:fingerprint state))]
    ((:restore r-def) state parent r-def)))

(defn serialize-receptors
  [receptors]
  [(into {} (map (fn [[k v]] {k (state v true)}) (filter (fn [[k v]] (= (class k) java.lang.Integer)) @receptors))) (:last-address @receptors)])

(defn unserialize-receptors [[states last-address]]
  (let [r (ref {:last-address last-address})]
    (doseq [[addr state] states]
      (dosync (alter r assoc addr (receptor-restore state nil))))
    r
    )
  )
(def *server-state-file-name* "anansi-server.state")
(def *receptors* (ref {})
     )

(defn load-receptors
  []
  (if (some #{*server-state-file-name*} (.list (java.io.File. ".")))
    (let [f (slurp *server-state-file-name*)]
      (if f
        (rsync nil (alter *receptors* merge @(unserialize-receptors (with-in-str f (read)))))
        )
      )
    ))

(defn find-receptors [receptor f]
  "utility function to search the list of receptors by an arbitrary function"
  (let [rc @(:receptors @receptor)]
    (map (fn [[_ v]] v) (filter (fn [[k r]] (and (not= k :last-address) (or (f r)) )) rc)))
  )
