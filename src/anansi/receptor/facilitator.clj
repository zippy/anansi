(ns
  #^{:author "Eric Harris-Braun"
     :doc "Facilitator receptor"}
  anansi.receptor.facilitator
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape]))

(defmethod manifest :facilitator [_r img_url]
           {:image-url img_url
            :stick-scape (receptor scape _r)})

(signal participant request-stick [_r participant-address]
        (let [stick (contents _r :stick-scape)]
          (if (= [] (address->resolve stick :have-it))
            (key->set stick participant-address :have-it)
            (key->set stick participant-address :want-it))))

(signal participant release-stick [_r participant-address]
        (let [stick (contents _r :stick-scape)
              want-it (address->resolve stick :want-it)]
          (dosync 
           (key->delete stick participant-address)
           (if (not= [] want-it)
             (key->set stick (first want-it) :have-it)))))

(signal matrice give-stick [_r participant-address]
        (let [stick (contents _r :stick-scape)]
          (dosync  (address->delete stick :have-it)
                   (key->set stick participant-address :have-it))
))
