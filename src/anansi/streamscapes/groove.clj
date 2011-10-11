(ns
  #^{:author "Eric Harris-Braun"
     :doc "Groove receptor"}
  anansi.streamscapes.groove
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        ))

(def groove-def (receptor-def "groove" (attributes :grammars)))
