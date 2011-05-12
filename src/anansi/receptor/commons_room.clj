(ns
  #^{:author "Eric Harris-Braun"
     :doc "Commons-room receptor"}
  anansi.receptor.commons-room
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.receptor.list-scape])
  (:use [anansi.receptor.portal])
  (:use [anansi.receptor.object])
  (:use [anansi.receptor.facilitator]))

(defmethod manifest :commons-room [_r]
           {:objects (ref {})
            :coords-scape (receptor scape _r)
            :occupant-scape (receptor scape _r)
            :seat-scape (receptor list-scape _r)
            :door (receptor portal _r)
            :door-log (ref [])
            :talking-stick (receptor facilitator _r "")})

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

;;; DOOR signals
(signal door enter [_r {unique-name :name occupant-data :data}]
        (dosync
         (let [o (anansi.receptor.portal/self->enter (contents _r :door) unique-name occupant-data)
               seats (contents _r :seat-scape)
               occupants (contents _r :occupant-scape)
               addr (address-of o)]
           (if (key->resolve occupants unique-name) (throw (RuntimeException. (str "'" unique-name "' is already in the room"))))
           (alter (contents _r :door-log) conj {:who unique-name, :what "entered", :when (java.util.Date.)})
           (comment address->push seats addr)
           (key->set occupants unique-name addr)
           o)
         ))

(defn resolve-occupant [occupants name]
  (let [addr (key->resolve occupants name)]
    (if (nil? addr) (throw (RuntimeException. (str "'" name "' is not in room"))))
    addr))

(signal door leave [_r unique-name]
        (dosync
         (let [seats (contents _r :seat-scape)
               occupants (contents _r :occupant-scape)
               addr (resolve-occupant occupants unique-name)]
           (alter (contents _r :door-log) conj {:who unique-name, :what "left", :when (java.util.Date.)})
           (comment address->delete seats addr)
           (address->delete occupants addr)
           (destroy-receptor _r addr))
         ))

;; TODO, I think the _r and unique-name parameters should be gensymed
;; but I don't know how to do that yet.
(defmacro forward-stick-signal [signal aspect name]
  `(signal ~aspect ~name [~'_r ~'unique-name]
           (let [~'occupants (contents ~'_r :occupant-scape)
              ~'addr (resolve-occupant ~'occupants ~'unique-name)
              ~'stick (contents ~'_r :talking-stick)
              ]
          (~signal ~'stick ~'addr)
          )
           ))
(forward-stick-signal participant->request-stick stick request)
(forward-stick-signal participant->release-stick stick release)
(forward-stick-signal matrice->give-stick stick give)

(comment signal stick request [_r unique-name]
        (let [occupants (contents _r :occupant-scape)
              addr (resolve-occupant occupants unique-name)
              stick (contents _r :talking-stick)
              ]
          (participant->request-stick stick addr)
          ))

(comment signal stick release [_r unique-name]
        (let [occupants (contents _r :occupant-scape)
              addr (resolve-occupant occupants unique-name)
              stick (contents _r :talking-stick)
              ]
          (participant->release-stick stick addr)
          ))
