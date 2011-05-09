(ns
  #^{:author "Eric Harris-Braun"
     :doc "List scape receptor"}
  anansi.receptor.list-scape
  (:use [anansi.ceptr]))

(defmethod manifest :list-scape [_r]
           {:map (ref [])} )

(signal address push [_r value]
    (dosync (alter (contents _r :map) conj value))
    (- (count @(contents _r :map)) 1)
        )
