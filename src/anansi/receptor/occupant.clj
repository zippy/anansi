(ns
  #^{:author "Eric Harris-Braun"
     :doc "Occupant receptor"}
  anansi.receptor.occupant
  (:use [anansi.ceptr]))

(defmethod manifest :occupant [_r unique-name data]
           {:unique-name unique-name :data data})
(defmethod state :occupant [_r full?]
           (assoc (state-convert _r full?)
             :unique-name (contents _r :unique-name)
             :data (contents _r :data)))
(defmethod restore :occupant [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :unique-name (:unique-name state))
             (restore-content r :data (:data state))
             r))

;(attribute (manifestable :picture))  ; FIXME add default picture of silhouette 
;(attribute (manifestable :full-name))
;(attribute (manifestable :contact-info))

