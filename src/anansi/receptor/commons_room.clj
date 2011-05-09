(ns
  #^{:author "Eric Harris-Braun"
     :doc "Commons-room receptor"}
  anansi.receptor.commons-room
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.receptor.list-scape])
  (:use [anansi.receptor.portal])
  (:use [anansi.receptor.object]))

(defmethod manifest :commons-room [_r]
           {:objects (ref {})
            :coords-scape (receptor scape _r)
            :occupant-scape (receptor scape _r)
            :seat-scape (receptor list-scape _r)
            :door (receptor portal _r)
            :door-log (ref [])})


;;; MATRICE signals
;; TODO
;; handle collisions when incorporating things onto the same place?
(signal matrice move [_r address x y]
        (let [coords (contents _r :coords-scape)]
          (address->delete coords address)
          (key->set coords [x y] address)))

(signal matrice incorporate [_r name picture_url x y]
        (let [o (receptor object _r picture_url)
              addr (address-of o)
              coords (contents _r :coords-scape)
              objects (contents _r :objects)]
          (dosync (alter objects assoc name o)
                  (matrice->move _r addr x y))
          addr))

(signal door enter [_r unique-name occupant-data]
        (dosync
         (let [o (anansi.receptor.portal/self->enter (contents _r :door) unique-name occupant-data)
               seats (contents _r :seat-scape)
               occupants (contents _r :occupant-scape)
               addr (address-of o)]
           (if (key->resolve occupants unique-name) (throw RuntimeException (str "name '" unique-name "' already in room")))
           (alter (contents _r :door-log) conj {:who unique-name, :what "entered", :when (java.util.Date.)})
           (comment address->push seats addr)
           (key->set occupants unique-name addr)
           o)
         ))

(signal door leave [_r unique-name]
        (dosync
         (let [seats (contents _r :seat-scape)
               occupants (contents _r :occupant-scape)
               addr (key->resolve occupants unique-name)]
           (if (nil? addr) (throw RuntimeException (str "name '" unique-name "' not in room")))
           (alter (contents _r :door-log) conj {:who unique-name, :what "left", :when (java.util.Date.)})
           (comment address->delete seats addr)
           (address->delete occupants addr))
         ))


