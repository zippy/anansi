(ns anansi.test.helpers
  (:use [clojure.contrib.io :only [writer]])
  (:use [clojure.test])
  (:use
   [anansi.ceptr]
   [anansi.receptor.host]
   [anansi.server-constants]
   [anansi.server]
   [anansi.user])
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader))
  )

(declare conn-handler)

(def *result* (ref []))

(defn connect [host port]
  (let [socket (Socket. host port)
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:in in :out out :socket socket})]
    (doto (Thread. #(conn-handler conn)) (.start))
    conn))

(defn write [conn msg]
  (doto (:out @conn)
    (.print (str msg "\n"))
    (.flush)))

(defn conn-handler [conn]
  (while 
      (nil? (:exit @conn))
    (let [msg (.readLine (:in @conn))]
      (if
          (not (nil? msg)) (dosync (alter *result* conj msg))))))
;
;(defn make-client-server
;  "Create a server and a client for testing purposes.
;Returns a three item vector of a server receptor, a writable stream that is a client, and the output stream of the server"
;  ([] (make-client-server "server"))
;  ([server-name]
;     (let [out (java.io.StringWriter.)
;           client-stream (java.io.PipedWriter.)
;           r (java.io.BufferedReader. (java.io.PipedReader. client-stream))
;           thread (Thread. #(binding [*server-state-file-name* "testing-server.state"
;                                      *done* false
;                                      *changes* (ref 0)
;                                      *user-name* "eric"
;                                      *receptors* (ref {})
;                                      *signals* (ref {})
;                                      ]
;                              (make-receptor host-def nil {})
;                              (anansi.server/anansi-handle-client r out)))
;           ]
;       (doto thread  .start)
;       [client-stream out])
;     ))

;(defmacro def-command-test [name & body]
; `(deftest ~name
;    (binding [*done* false
;              *changes* (ref 0)
;              *user-name* "eric"
;              *receptors* (ref {})
;              *signals* (ref {})
;;;               *room-addr* (s-> self->host-room *host* "the room" )
;              *server-state-file-name* "testing-server.state"
;              *print-level* 10]
;      (do (make-receptor host-def nil {})
;          (dosync  (commute user-streams assoc *user-name* (get-receptor (get-host) (s-> self->host-user (get-host) *user-name*)))))
;      ~@body)))

(comment import '(java.util.concurrent Excecutors))
(comment def *pool* (Exe cutors/newFixedThreadPool
                 (+ 2 (.availableProcessors (Runtime/getRuntime)))))
(comment defn dothreads! [f & {thread-count :threads
                       exec-count :times
                       :or {thread-count 1 exec-count 1}}]
  (dotimes [t thread-count]
    (.submit *pool* #(dotimes [_ exec-count] (f)))))
