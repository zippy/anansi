(ns
  #^{:author "Eric Harris-Braun"
     :doc "Facilitator receptor"}
  anansi.receptor.facilitator
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape]))

(defmethod manifest :facilitator [_r img_url]
           (make-scapes _r {:image-url img_url} :stick))

(defmethod state :facilitator [_r full?]
           (assoc (state-convert _r full?)
             :image-url (contents _r :image-url)))
(defmethod restore :facilitator [state parent]
           (let [r (do-restore state parent)]
             (set-content r :image-url (:image-url state))
             r))

(signal participant request-stick [_r _f participant-address]
        (let [stick (contents _r :stick-scape)]
          (if (= [] (--> address->resolve _r stick :have-it))
            (--> key->set _r stick participant-address :have-it)
            (--> key->set _r stick participant-address :want-it))))

(signal participant release-stick [_r _f participant-address]
        (let [stick (contents _r :stick-scape)
              want-it (--> address->resolve _r stick :want-it)]
          (dosync 
           (--> key->delete _r stick participant-address)
           (if (not= [] want-it)
             (--> key->set _r stick (first want-it) :have-it)))))

(signal matrice give-stick [_r _f participant-address]
        (let [stick (contents _r :stick-scape)]
          (dosync  (--> address->delete _r stick :have-it)
                   (--> key->set _r stick participant-address :have-it))
))
