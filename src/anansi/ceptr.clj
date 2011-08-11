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

(defmacro signal [aspect name args & body]
  "Creates a signal function on an aspect"
  `(do
     (defn ~(symbol (str aspect "->" name)) ~args ~@body)
     (dosync (alter *signals* assoc (keyword (str (ns-name *ns*) "." '~aspect "." '~name)) '~args))))

(defmulti manifest (fn [receptor & args] (:type @receptor)))
(defmethod manifest :default [receptor & args] {})

(defmulti state (fn [receptor full?] (:type @receptor)))
(defmulti restore (fn [s p] (:type s)))

(defn receptors-container [receptor] (if (nil? receptor) *receptors* (:receptors @receptor)))

(defmacro receptor [name parent & args]
  "Instantiates a receptor"
  `(let [~'ns-str :last-address ;;(keyword (str (ns-name *ns*) "." '~name))
         ~'receptors (receptors-container ~parent)
         ~'c (~'ns-str @~'receptors)
         ~'addr (if (nil? ~'c) 1 (+ ~'c 1))
         ~'type (keyword '~name)
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
            ~'r
            )))

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
  @(contents (contents _r scape-name) :map))

(defn _get-scape [receptor scape-name]
  (contents receptor (scapify scape-name))
  )

(defn state-convert
  "worker function to serialize the standard contents of a recptor"
  [receptor full?]
  (let [r @receptor
        rc @(:receptors r)
        s1 {:type (:type r),
            :address (:address r)
            :changes (:changes r)
            }
        s (if (empty? rc) s1
              (assoc s1
                :receptors (assoc (modify-vals (fn [x] (state x full?))  (filter (fn [[k v]] (and (not= k :last-address) (or full? (not= (:type @v) :scape)) )) rc))
                             :last-address (:last-address rc))))
        ss (_get-scape receptor :scapes)]
    (if (and (not full?) ss)
      (let [scapes (keys @(contents ss :map))] ;; this is cheating 
        (assoc s :scapes (into {} (map (fn [sn] [sn (scape-state receptor sn)] ) scapes))))
      (if ss (assoc s :scapes-scape-addr (address-of ss)) s)
      )))
(defmethod state :default [receptor full?]
           (state-convert receptor full?))

(defn do-restore [state parent]
  (let [r (ref {})
        rc (:receptors state)
        sr (modify-vals (fn [s] (restore s r)) (filter (fn [[k v]] (not= k :last-address)) rc))
        sr1 (if (:last-address rc) (assoc sr :last-address (:last-address rc)) sr)
        c {}
        r1 {:type (:type state)
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
    r)
  )

(defmethod restore :default [state parent] (do-restore state parent) )

(defn serialize-receptors
  [receptors]
  [(into {} (map (fn [[k v]] {k (state v true)}) (filter (fn [[k v]] (= (class k) java.lang.Integer)) @receptors))) (:last-address @receptors)])

(defn unserialize-receptors [[states last-address]]
  (let [r (ref {:last-address last-address})]
    (doseq [[addr state] states]
      (dosync (alter r assoc addr (restore state nil))))
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

