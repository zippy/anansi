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

(defmethod manifest :commons-room [_r matrice-address password]
           (let [ms (receptor scape _r)]
             (s-> key->set ms matrice-address :matrice)
             (make-scapes _r  {:password password
                               :objects (ref {})
                               :matrice-scape ms
                               :door (receptor portal _r)
                               :door-log (ref [])
                               :talking-stick (receptor facilitator _r "")}
                          :agent :coords :occupant :status)))

(defmethod state :commons-room [_r full?]
           (let [base-state (state-convert _r full?)]
             (if full?
               (assoc base-state
                 :password (contents _r :password)
                 ;; TODO object state
                 :matrice-scape (address-of (contents _r :matrice-scape))
                 :door (address-of (contents _r :door))
                 :talking-stick (address-of (contents _r :talking-stick))
                 :door-log @(contents _r :door-log)
                 )
               (assoc base-state 
                   ;;             :objects (map state @(contents _r :objects))
                   :matrices (s-> key->all (contents _r :matrice-scape))
                   :talking-stick (state (contents _r :talking-stick) full?)
                   )))
           )
(defmethod restore :commons-room [state parent]
           (let [r (do-restore state parent)]
             (set-content r :objects (ref {})) ;TODO actually restore the objects...
             (set-content r :password (:password state))
             (set-content r :matrice-scape (get-receptor r (:matrice-scape state)))
             (set-content r :door (get-receptor r (:door state)))
             (set-content r :door-log (ref (:door-log state)))
             (set-content r :talking-stick (get-receptor r (:talking-stick state)))
             r))

;;; MATRICE signals
;; TODO
;; handle collisions when incorporating things onto the same place?

(defn matrice? [_r _f]
  (= :matrice (--> key->resolve _r (contents  _r :matrice-scape) _f )))

(defn agent-or-matrice? [_r _f occupant-address]
  (or
   (= _f (--> key->resolve _r (contents  _r :agent-scape) occupant-address))
   (matrice? _r _f)))

(defn do-move [_r address x y]
 ( let [coords (contents _r :coords-scape)]
   (--> address->delete _r coords address)
   (--> key->set _r coords [x y] address)))

(signal matrice move [_r _f {address :addr x :x y :y}]
        (if (matrice? _r _f)
          (do-move _r address x y)
          (throw (RuntimeException. "not matrice"))
          ))
(signal matrice update-status [_r _f {address :addr  status :status}]
        (if (agent-or-matrice? _r _f address)
          (do (--> key->set _r (contents _r :status-scape) address (keyword status)) nil)
          (throw (RuntimeException. "no agency"))
          ))

(signal matrice incorporate [_r _f name picture_url x y]
        (let [o (receptor object _r picture_url)
              addr (address-of o)
              coords (contents _r :coords-scape)
              objects (contents _r :objects)]
          (rsync (alter objects assoc name o)
                  ( do-move _r addr x y)
                  )
          addr))

(defn- check-password [_r password]
  (= (contents _r :password) password ))

;;; DOOR signals
(signal door enter [_r _f {unique-name :name occupant-data :data password :password}]
        (rsync
         (let [o (--> anansi.receptor.portal/self->enter _r (contents _r :door) unique-name occupant-data)
               seats (contents _r :seat-scape)
               occupants (contents _r :occupant-scape)
               addr (address-of o)]
           (if (not (check-password _r password)) (throw (RuntimeException. "incorrect room password")))
           (if (--> key->resolve _r occupants unique-name) (throw (RuntimeException. (str "'" unique-name "' is already in the room"))))
           (alter (contents _r :door-log) conj {:who unique-name, :what "entered", :when (java.util.Date.)})
           (--> key->set _r (contents _r :agent-scape) addr _f)
           (--> key->set _r (contents _r :status-scape) addr :present)
           (comment address->push seats addr)
           (--> key->set _r occupants unique-name addr)
           (address-of o))
         ))

(defn resolve-occupant [_r occupants name]
  (let [addr (--> key->resolve _r occupants name)]
    (if (nil? addr) (throw (RuntimeException. (str "'" name "' is not in room"))))
    addr))

(signal door leave [_r _f unique-name]
        (rsync
         (let [seats (contents _r :seat-scape)
               occupants (contents _r :occupant-scape)
               agents (contents _r :agent-scape)
               status (contents _r :status-scape)
               addr (resolve-occupant _r occupants unique-name)]
           (if (not ( agent-or-matrice? _r _f addr)) (throw (RuntimeException. "no agency")))
           (alter (contents _r :door-log) conj {:who unique-name, :what "left", :when (java.util.Date.)})
           (comment address->delete seats addr)
           (--> address->delete _r occupants addr)
           (--> key->delete _r agents addr)
           (--> key->delete _r status addr)
           (destroy-receptor _r addr)
           nil)))

;; TODO, I think the _r and unique-name parameters should be gensymed
;; but I don't know how to do that yet.
(defmacro forward-stick-signal [signal aspect name]
  `(signal ~aspect ~name [~'_r ~'_f ~'unique-name]
           (let [~'occupants (contents ~'_r :occupant-scape)
              ~'addr (resolve-occupant ~'_r ~'occupants ~'unique-name)
              ~'stick (contents ~'_r :talking-stick)
              ]
             (if (not (agent-or-matrice? ~'_r ~'_f ~'addr)) (throw (RuntimeException. "no agency")))
             (--> ~signal ~'_r ~'stick ~'addr)
          )
           ))
(forward-stick-signal participant->request-stick stick request)
(forward-stick-signal participant->release-stick stick release)
(forward-stick-signal matrice->give-stick stick give)

(comment signal stick request [_r _f unique-name]
        (let [occupants (contents _r :occupant-scape)
              addr (resolve-occupant _r occupants unique-name)
              stick (contents _r :talking-stick)
              ]
          (--> participant->request-stick _r stick addr)
          ))

(comment signal stick release [_r _f unique-name]
        (let [occupants (contents _r :occupant-scape)
              addr (resolve-occupant _r occupants unique-name)
              stick (contents _r :talking-stick)
              ]
          (--> participant->release-stick _r stick addr)
          ))
