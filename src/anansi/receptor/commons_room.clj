(ns
  #^{:author "Eric Harris-Braun"
     :doc "Commons-room receptor"}
  anansi.receptor.commons-room
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.receptor.list-scape])
  (:use [anansi.receptor.portal])
  (:use [anansi.receptor.object])
  (:use [anansi.receptor.facilitator]
        [anansi.receptor.occupant]))

(defmethod manifest :commons-room [_r matrice-address password data]
           (let [ms (receptor scape _r)]
             (s-> key->set ms matrice-address :matrice)
             (make-scapes _r  {:password password
                               :objects (ref {})
                               :matrice-scape ms
                               :door (receptor portal _r)
                               :door-log (ref [])
                               :talking-stick (receptor facilitator _r "")
                               :data data
                               }
                          :agent :coords :occupant :status :chair)))

(defmethod state :commons-room [_r full?]
           (let [base-state (state-convert _r full?)]
             (if full?
               (assoc base-state
                 :password (contents _r :password)
                 ;; TODO object state
                 :matrice-scape (address-of (get-scape _r :matrice))
                 :door (address-of (contents _r :door))
                 :talking-stick (address-of (contents _r :talking-stick))
                 :door-log @(contents _r :door-log)
                 :data (contents _r :data)
                 )
               (assoc base-state 
                   ;;             :objects (map state @(contents _r :objects))
                 :data (contents _r :data)
                 :matrices (s-> key->all (get-scape _r :matrice))
                 :talking-stick (state (contents _r :talking-stick) full?)
                 :occupants (into {} (map (fn [[name addr]] [name (:data (state (get-receptor _r addr) false))]) @(contents (get-scape _r :occupant) :map)))
                   )))
           )
(defmethod restore :commons-room [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :objects (ref {})) ;TODO actually restore the objects...
             (restore-content r :password (:password state))
             (restore-content r :matrice-scape (get-receptor r (:matrice-scape state)))
             (restore-content r :door (get-receptor r (:door state)))
             (restore-content r :door-log (ref (:door-log state)))
             (restore-content r :talking-stick (get-receptor r (:talking-stick state)))
             (restore-content r :data (:data state))
             r))

;;; MATRICE signals
;; TODO
;; handle collisions when incorporating things onto the same place?

(defn matrice? [_r _f]
  (= :matrice (--> key->resolve _r (get-scape  _r :matrice) _f )))

(defn agent-or-matrice? [_r _f occupant-address]
  (or
   (= _f (--> key->resolve _r (get-scape  _r :agent) occupant-address))
   (matrice? _r _f)))

(defn do-move [_r address x y]
 ( let [coords (get-scape _r :coords)]
   (rsync _r
    (--> address->delete _r coords address)
    (--> key->set _r coords [x y] address))))

(signal matrice move [_r _f {address :addr x :x y :y}]
        (if (matrice? _r _f)
          (do-move _r address x y)
          (throw (RuntimeException. "not matrice"))
          ))

(signal matrice sit [_r _f {address :addr c :chair}]
        (if (matrice? _r _f)
          ( let [chairs (get-scape _r :chair)]
            (rsync _r
                   (--> address->delete _r chairs address)
                   (--> key->set _r chairs c address)))
          (throw (RuntimeException. "not matrice"))
          ))

(signal matrice make-matrice [_r _f {address :addr}]
        (if (matrice? _r _f)
          (rsync _r
                 (--> key->set _r (get-scape _r :matrice) address :matrice)
                 )
          (throw (RuntimeException. "not matrice"))
          ))

(signal matrice make-agent [_r _f {occupant :occupant address :addr}]
        (if (matrice? _r _f)
          (rsync _r
                 (--> key->set _r (get-scape _r :agent) occupant address)
                 )
          (throw (RuntimeException. "not matrice"))
          ))

(signal matrice update-status [_r _f {address :addr  status :status}]
        (if (agent-or-matrice? _r _f address)
          (rsync _r (--> key->set _r (get-scape _r :status) address (keyword status)) nil)
          (throw (RuntimeException. "no agency"))
          ))

(signal matrice incorporate [_r _f name picture_url x y]
        (let [o (receptor object _r picture_url)
              addr (address-of o)
              coords (get-scape _r :coords)
              objects (contents _r :objects)]
          (rsync _r (alter objects assoc name o)
                  ( do-move _r addr x y)
                  )
          addr))

(defn- check-password [_r password]
  (= (contents _r :password) password ))

;;; DOOR signals
(signal door enter [_r _f {unique-name :name occupant-data :data password :password}]
        (rsync _r
         (let [o (--> anansi.receptor.portal/self->enter _r (contents _r :door) unique-name occupant-data)
               ; seats (get-scape _r :seat)
               occupants (get-scape _r :occupant)
               addr (address-of o)]
           (if (not (check-password _r password)) (throw (RuntimeException. "incorrect room password")))
           (if (--> key->resolve _r occupants unique-name) (throw (RuntimeException. (str "'" unique-name "' is already in the room"))))
           (alter (contents _r :door-log) conj {:who unique-name, :what "entered", :when (.toString (java.util.Date.))})
           (--> key->set _r (get-scape _r :agent) addr _f)
           (--> key->set _r (get-scape _r :status) addr :present)
           ; (address->push seats addr)
           (--> key->set _r occupants unique-name addr)
           (address-of o))
         ))

(defn resolve-occupant [_r occupants name]
  (let [addr (--> key->resolve _r occupants name)]
    (if (nil? addr) (throw (RuntimeException. (str "'" name "' is not in room"))))
    addr))

(signal door leave [_r _f unique-name]
        (rsync _r
         (let [;seats (get-scape _r :seat)
               occupants (get-scape _r :occupant)
               agents (get-scape _r :agent)
               status (get-scape _r :status)
               addr (resolve-occupant _r occupants unique-name)]
           (if (not ( agent-or-matrice? _r _f addr)) (throw (RuntimeException. "no agency")))
           (alter (contents _r :door-log) conj {:who unique-name, :what "left", :when (.toString (java.util.Date.))})
           ;(comment address->delete seats addr)
           (--> address->delete _r occupants addr)
           (--> key->delete _r agents addr)
           (--> key->delete _r status addr)
           (destroy-receptor _r addr)
           nil)))

;; TODO, I think the _r and unique-name parameters should be gensymed
;; but I don't know how to do that yet.
(defmacro forward-stick-signal [signal aspect name]
  `(signal ~aspect ~name [~'_r ~'_f ~'unique-name]
           (let [~'occupants (get-scape ~'_r :occupant)
              ~'addr (resolve-occupant ~'_r ~'occupants ~'unique-name)
              ~'stick (contents ~'_r :talking-stick)
              ]
             (if (not (agent-or-matrice? ~'_r ~'_f ~'addr)) (throw (RuntimeException. "no agency")))
             (rsync ~'_r (--> ~signal ~'_r ~'stick ~'addr))
          )
           ))
(forward-stick-signal participant->request-stick stick request)
(forward-stick-signal participant->release-stick stick release)
(forward-stick-signal matrice->give-stick stick give)

(comment signal stick request [_r _f unique-name]
        (let [occupants (get-scape _r :occupant)
              addr (resolve-occupant _r occupants unique-name)
              stick (contents _r :talking-stick)
              ]
          (--> participant->request-stick _r stick addr)
          ))

(comment signal stick release [_r _f unique-name]
        (let [occupants (get-scape _r :occupant)
              addr (resolve-occupant _r occupants unique-name)
              stick (contents _r :talking-stick)
              ]
          (--> participant->release-stick _r stick addr)
          ))


(signal occupant update-data [_r _f {name :name  data :data key :key}]
        (let [occupants (get-scape _r :occupant)
              addr (resolve-occupant _r occupants name)]
          (if (agent-or-matrice? _r _f addr)
            (rsync _r (--> self->update _r (get-receptor _r addr) data key))
            (throw (RuntimeException. "no agency"))
            )))

