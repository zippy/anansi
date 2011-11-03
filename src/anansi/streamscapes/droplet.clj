(ns
  #^{:author "Eric Harris-Braun"
     :doc "Droplet receptor"}
  anansi.streamscapes.droplet
  (:use [anansi.ceptr]))

(def droplet-def (receptor-def "droplet"
                               (attributes :id :from :to :channel :envelope :content :matched-grooves)
                               (manifest [_r id from to channel envelope content]
                                         {:id (if (or (nil? id) (= id "")) (str (address-of (parent-of _r)) "." (address-of _r)) id) :from from :to to :channel channel :envelope envelope :content content})))
