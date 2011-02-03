(ns anansi.test.helpers
  (:use [clojure.contrib.io :only [writer]])
  )

(defn make-client-server
  "Create a server and a client for testing purposes.
Returns a two item vector of a server receptor and a writable stream that is a client"
  ([] (make-client-server "server"))
  ([server-name]
     (let [*out* (java.io.PrintWriter. (writer "/dev/null"))
           server (anansi.receptor/make-server server-name)
           client-stream (java.io.PipedWriter.)
           r (java.io.BufferedReader. (java.io.PipedReader. client-stream))]
       (doto (Thread. #(do (anansi.server/anansi-handle-client r *out*))) .start)
       [server client-stream])
     ))
