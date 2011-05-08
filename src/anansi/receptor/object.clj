(ns
  #^{:author "Eric Harris-Braun"
     :doc "Object receptor"}
  anansi.receptor.object
  (:use [anansi.ceptr]))

(defmethod manifest :object [_r img_url]
           {:image-url img_url})

