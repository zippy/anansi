(ns
  #^{:author "Eric Harris-Braun"
     :doc "User receptor"}
  anansi.receptor.user
  (:use [anansi.ceptr]))

(defmethod manifest :user [_r name stream]
           {:name name
            :stream stream})

(signal self disconnect [_r]
        (set-content _r :stream nil))

(signal self connect [_r stream]
        (set-content _r :stream stream))
