(ns
  #^{:author "Eric Harris-Braun"
     :doc "Scape receptor"}
  anansi.receptor.scape
  (:use [anansi.ceptr])
  (:use [anansi.map-utilities]))

(defmethod manifest :scape [_r]
  {:map (ref (sorted-map))} )

;; Signals on the key aspect
(signal key set [_r key value]
        (dosync (alter (contents _r :map) assoc key value)))
(signal key resolve [_r key]
        (get @(contents _r :map) key))
(signal key all [_r]
        (into [] (keys @(contents _r :map))))
(signal key delete [_r key]
        (dosync (alter (contents _r :map) dissoc key)))

;; Signals on the address aspect
(signal address resolve [_r address]
        (get-keys @(contents _r :map) address))
(signal address all [_r]
        (into [] (distinct (vals @(contents _r :map)))))
(signal address delete [_r address]
        (dosync (alter (contents _r :map) remove-value address)))
