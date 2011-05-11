(ns
  #^{:author "Eric Harris-Braun"
     :doc "User receptor"}
  anansi.receptor.user
  (:use [anansi.ceptr]))

(defmethod manifest :user [_r name stream]
           {:name name
            :stream stream})

