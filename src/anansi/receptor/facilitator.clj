(ns
  #^{:author "Eric Harris-Braun"
     :doc "Facilitator receptor"}
  anansi.receptor.facilitator
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape]))

(defmethod manifest :facilitator [_r img_url]
           {:image-url img_url
            :stick-scape (receptor scape _r)})

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
