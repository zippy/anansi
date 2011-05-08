(ns
  #^{:author "Eric Harris-Braun"
     :doc "Occupant receptor"}
  anansi.receptor.occupant
  (:use [anansi.ceptr]))

(defmethod initialize-contents :occupant [x]
           {})

;(attribute (manifestable :picture))  ; FIXME add default picture of silhouette 
;(attribute (manifestable :full-name))
;(attribute (manifestable :contact-info))

