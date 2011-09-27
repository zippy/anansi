(ns
  #^{:author "Eric Harris-Braun"
     :doc "Droplet receptor"}
  anansi.streamscapes.droplet
  (:use [anansi.ceptr]))

(def droplet-def (receptor-def "droplet"
                               (attributes :id :from :to :aspect :envelope :content)
                               (manifest [_r id from to aspect envelope content]
                                         {:id (if (or (nil? id) (= id "")) (str (address-of (parent-of _r)) "." (address-of _r)) id) :from from :to to :aspect aspect :envelope envelope :content content})))
