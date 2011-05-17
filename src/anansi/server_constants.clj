(ns
    #^{:skip-wiki true}
  anansi.server-constants
  (:use [anansi.receptor :only [make-server unserialize-receptor]]
        [anansi.receptor.host]
        [anansi.ceptr])  )

(def *host* (receptor host nil))
(comment def *room-addr* (s-> self->host-room *host* "the room" ))
(def *server-receptor* (make-server "server")
     )

(defn get-host []
  (get-receptor nil 1)
  )
