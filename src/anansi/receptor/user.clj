(ns
  #^{:author "Eric Harris-Braun"
     :doc "User receptor"}
  anansi.receptor.user
  (:use [anansi.ceptr]
        [anansi.receptor.scape]))

(def user-def (receptor-def "user"
                            (attributes :name)
                            (manifest [_r name] {:name name})
                            ))

(signal self disconnect [_r _f]
        (set-content _r :stream nil))

(signal self connect [_r _f stream]
        (set-content _r :stream stream))
