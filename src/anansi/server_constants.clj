(ns
    #^{:skip-wiki true}
  anansi.server-constants
  (:use [anansi.receptor :only [make-server unserialize-receptor]]
        [anansi.receptor.host]
        [anansi.ceptr])  )

(def *host* (receptor host nil))
(def *room-addr* (s-> self->host-room *host* "the room" ))
(def *server-state-file-name* "anansi-server.state")
(def *server-receptor*
     (if (some #{*server-state-file-name*} (.list (java.io.File. ".")))
       (unserialize-receptor (slurp *server-state-file-name*))
       (make-server "server")))

