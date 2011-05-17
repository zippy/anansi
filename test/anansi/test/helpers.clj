(ns anansi.test.helpers
  (:use [clojure.contrib.io :only [writer]])
  (:use [clojure.test])
  (:use
   [anansi.ceptr]
   [anansi.receptor.host]
   [anansi.server-constants]
   [anansi.server]
   [anansi.user])
  )

(defn make-client-server
  "Create a server and a client for testing purposes.
Returns a three item vector of a server receptor, a writable stream that is a client, and the output stream of the server"
  ([] (make-client-server "server"))
  ([server-name]
     (let [out (java.io.StringWriter.)
           client-stream (java.io.PipedWriter.)
           r (java.io.BufferedReader. (java.io.PipedReader. client-stream))
           thread (Thread. #(do (anansi.server/anansi-handle-client r out)))
           ]
       (doto thread  .start)
       [client-stream out])
     ))

(defmacro def-command-test [name & body]
 `(deftest ~name
    (binding [*done* false
              *user-name* "eric"
              *receptors* (ref {})
              *signals* (ref {})
;;               *room-addr* (s-> self->host-room *host* "the room" )
              *server-state-file-name* "testing-server.state"
              *print-level* 10]
      (do (receptor ~'host nil)
          (dosync  (commute user-streams assoc *user-name* (get-receptor (get-host) (s-> self->host-user (get-host) *user-name*)))))
      ~@body)))
