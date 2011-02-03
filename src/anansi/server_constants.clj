(ns
  #^{:author "Eric Harris-Braun"
     :doc "constants used by the server"}  
    anansi.server-constants
  (:use [anansi.receptor :only [make-server unserialize-receptor]])  )

(def *server-state-file-name* "anansi-server.state")
(def *server-receptor*
     (if (some #{*server-state-file-name*} (.list (java.io.File. ".")))
       (unserialize-receptor (slurp *server-state-file-name*))
       (make-server "server")))

