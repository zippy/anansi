(ns
  #^{:author "Eric Harris-Braun"
     :doc "Occupant receptor"}
  anansi.receptor.occupant
  (:use [anansi.ceptr]))

(defmethod manifest :occupant [_r unique-name data]
           {:unique-name unique-name :data data})

;(attribute (manifestable :picture))  ; FIXME add default picture of silhouette 
;(attribute (manifestable :full-name))
;(attribute (manifestable :contact-info))

