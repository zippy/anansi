(ns
  #^{:author "Eric Harris-Braun"
     :doc "Portal receptor"}
  anansi.receptor.portal
  (:use [anansi.ceptr]))

(defmethod manifest :portal [_r & [default]]
    {:target (if (nil? default) (parent-of _r) default)} )

(signal self enter [_r unique-name occupant-data]
        (receptor occupant (contents _r :target) unique-name occupant-data)
        )
