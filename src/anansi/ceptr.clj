(ns
  #^{:author "Eric Harris-Braun"
     :doc "basic ceptr functions"}
  anansi.ceptr
  (:use [anansi.map-utilities]
        [anansi.util]))

(def *signals* (ref {}))
(def *receptors* (ref {}))
(defn get-signal [s] {s @*signals*})

(defmacro signal [aspect name args & body]
  "Creates a signal function on an aspect"
  `(do
     (defn ~(symbol (str aspect "->" name)) ~args ~@body)
     (dosync (alter *signals* assoc (keyword (str (ns-name *ns*) "." '~aspect "." '~name)) '~args))))

(defmulti initialize-contents (fn [x & args] x))
(defmethod initialize-contents :default [x & args] {})

(defmulti manifest (fn [receptor & args] (:type @receptor)))
(defmethod manifest :default [receptor & args] {})

(defmulti state (fn [receptor] (:type @receptor)))
(defn state-convert [receptor]
  (let [r @receptor
        p (parent-of receptor)]
    {:type (:type r),
     :parent (if (nil? p) nil (address-of p))
     :address (:address r)
     :receptors (modify-vals (fn [x] (state x)) (dissoc @(:receptors r) :last-address))
     }))
(defmethod state :default [receptor]
           (state-convert receptor))

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
                   :address ~'addr})
         ]
     (dosync (alter ~'receptors assoc ~'ns-str ~'addr)
             (alter ~'receptors assoc ~'addr ~'r)
             (alter ~'r assoc :contents (ref (manifest ~'r ~@args)))
             ~'r
             )))

(defn contents
  "get an item out of the manifest"
  [receptor key] (key @(:contents @receptor)))

(defn set-content
  "set the value of a manifest item"
  [receptor key value] (dosync (alter (:contents @receptor) assoc key value)))

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
  (dosync ( alter (receptors-container receptor) dissoc address)))
