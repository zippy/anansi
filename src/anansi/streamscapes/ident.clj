(ns
  #^{:author "Eric Harris-Braun"
     :doc "Identity receptor"}
  anansi.streamscapes.ident
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        ))

(def ident-def (receptor-def "ident" (attributes :name)))
