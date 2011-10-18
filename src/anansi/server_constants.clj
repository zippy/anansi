(ns
    #^{:skip-wiki true}
  anansi.server-constants
  (:use [anansi.receptor.host]
        [anansi.ceptr])  )

(def *host* (make-receptor host-def nil {}))
(comment def *room-addr* (s-> self->host-room *host* "the room" ))

(defn get-host []
  (get-receptor nil 1)
  )
