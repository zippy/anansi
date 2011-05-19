(ns 
  #^{:skip-wiki true}    
  anansi.user)

(def *user-name*)

(def *prompt* (ref nil))
(def user-streams (ref {}))
(def *done*)
