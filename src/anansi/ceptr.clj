(ns
  #^{:author "Eric Harris-Braun"
     :doc "basic ceptr functions"}
  anansi.ceptr
  (:use [anansi.map-utilities]))

(def *signals* (ref {}))
(def *receptors* (ref {}))
(defn get-signal [s] {s @*signals*})

(defmacro signal [aspect name & body]
  "Creates a signal function on an aspect"
  `(do
     (defn ~(symbol (str aspect "->" name)) ~@body)
     (dosync (alter *signals* assoc (keyword (str (ns-name *ns*) "." '~aspect "." '~name)) 1))))

(defmulti initialize-contents (fn [x & args] x))
(defmethod initialize-contents :default [x & args] {})

(defmulti manifest (fn [receptor & args] (:type @receptor)))
(defmethod manifest :default [receptor & args] {})

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

(defn contents [receptor key] (key @(:contents @receptor)))
(defn parent-of [receptor] (:parent @receptor))
(defn send-sig [receptor signal & args]
  (apply signal receptor args))
(defn address-of [receptor] (:address @receptor))
(defn get-receptor [receptor address]
  (let [receptors @(receptors-container receptor)]
    (get receptors address)))
(defn destroy-receptor [receptor address]
  (dosync ( alter (receptors-container receptor) dissoc address)))
