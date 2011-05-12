(ns
  #^{:author "Eric Harris-Braun"
     :doc "User receptor"}
  anansi.receptor.user
  (:use [anansi.ceptr]))

(defmethod manifest :user [_r name stream]
           {:name name
            :stream stream})

(signal self disconnect [_r _f]
        (set-content _r :stream nil))

(signal self connect [_r _f stream]
        (set-content _r :stream stream))
