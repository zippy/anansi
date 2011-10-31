(ns
  #^{:author "Eric Harris-Braun"
     :doc "Contact receptor"}
  anansi.streamscapes.contact
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        ))

(def contact-def (receptor-def "contact" (attributes :name)))
