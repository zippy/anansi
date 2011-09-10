(ns anansi.test.receptor.host-interface.telnet
  (:use [anansi.receptor.host-interface.telnet] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.host])
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]))

(defn make-test-connection
  "Create a server and a client for testing purposes.
Returns a two item vector of a writable stream that is a client, and the output stream of the server"
  ([connection-function]
     (let [out (java.io.StringWriter.)
           client-stream (java.io.PipedWriter.)
           r (java.io.BufferedReader. (java.io.PipedReader. client-stream))
           thread (Thread. #(binding [*server-state-file-name* "testing-server.state"
                                      *done* false
                                      *changes* (ref 0)
                                      ;*user-name* "eric"
                                      *receptors* (ref {})
                                      *signals* (ref {})
                                      *err* (java.io.PrintWriter. (writer "/dev/null"))
                                      ]
                              (receptor :host nil)
                              (connection-function r out)))
           ]
       (doto thread  .start)
       [client-stream out])
     ))

(def *len* (ref 0))
(defn- len [s] (count (.toString s)))
(defn- setlen [s]
  (dosync (ref-set *len* (len s))))
(defn- wait [s]
  (while (= @*len* (len s)) (Thread/sleep 1))
  (setlen s)
  )

(deftest telnet-interface
  (let [h (receptor :host nil)
        r (receptor :telnet-host-interface h {})
        z-addr (s-> self->host-user h "zippy")
        [client-stream server-stream] (make-test-connection (make-handle-connection h r))
        ]
    (testing "welcome"
      (wait server-stream)
      (is  (.endsWith (.toString server-stream) "\nWelcome to the Anansi sever.\n\n> ")))
    (testing "authenticate"
      (.write client-stream "badcommand eric\n")
      (wait server-stream)
      (is (re-find #"ERROR Unknown command: 'badcommand'\n"(.toString server-stream) )))
    (testing "authenticate"
      (.write client-stream "authenticate eric\n")
      (wait server-stream)
      (is (.endsWith (.toString server-stream) "ERROR authentication failed for user: eric\n\n> "))
      (.write client-stream "authenticate zippy\n")
      (wait server-stream)
      (is (re-find #"OK [0-9a-f]+\n\n> $"(.toString server-stream) ))
      )
    ))
