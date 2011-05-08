(ns
  #^{:author "Eric Harris-Braun"
     :doc "Commons-room receptor"}
  anansi.receptor.commons-room
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.receptor.object]))

(defmethod manifest :commons-room [_r]
           {:objects (ref {})
            :coords-scape (receptor scape _r)})

;; TODO
;; handle collisions when incorporating things onto the same place?
(signal matrice incorporate [_r name picture_url x y]
        (let [o (receptor object _r picture_url)
              addr (address-of o)
              coords (contents _r :coords-scape)
              objects (contents _r :objects)]
          (dosync (alter objects assoc name o)
                  (key->set coords [x y] addr))
          addr))
