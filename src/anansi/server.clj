(ns
  #^{:skip-wiki true}    
   anansi.server
  (:use [anansi.receptor :only [make-server]])
  )

(def *server-receptor* (make-server "server"))
