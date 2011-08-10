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
             (restore-content r :image-url (:image-url state))
             r))

(signal participant request-stick [_r _f participant-address]
    (rsync _r
        (let [stick (get-scape _r :stick)]
          (if (= [] (--> address->resolve _r stick :have-it))
              (--> key->set _r stick participant-address :have-it)
              (--> key->set _r stick participant-address :want-it)))))

(signal participant release-stick [_r _f participant-address]
        (let [stick (get-scape _r :stick)
              want-it (--> address->resolve _r stick :want-it)
              have-it (--> address->resolve _r stick :have-it)]
          (rsync _r
                 (--> key->delete _r stick participant-address)
                 (if (not (some #{participant-address} want-it))
                   (if (not= [] want-it)
                     (--> key->set _r stick (first want-it) :have-it))))))

(signal matrice give-stick [_r _f participant-address]
        (let [stick (get-scape _r :stick)]
          (rsync  _r (--> address->delete _r stick :have-it)
                   (--> key->set _r stick participant-address :have-it))
))
