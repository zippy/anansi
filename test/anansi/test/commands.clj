(ns anansi.test.commands
  (:use [anansi.receptor]
        [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.host]
)
  (:use [anansi.server]
        [anansi.server-constants])
  (:use [anansi.user]
        [anansi.test.helpers])
  (:use [anansi.commands] :reload)
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]
        [clojure.contrib.json :only [json-str]]))

(defmacro def-command-test [name & body]
  `(deftest ~name
     (binding [*server-receptor* (make-server "server")
               *user-name* "eric"
               *receptors* (ref {})
               *signals* (ref {})
;;               *room-addr* (s-> self->host-room *host* "the room" )
               *server-state-file-name* "testing-server.state"]
       (dosync (commute user-streams assoc *user-name* (get-receptor *host* (s-> self->host-user *host* *user-name*))))
       ~@body)))

(def-command-test send-test
  (testing "sending a message"
    (is (= "created" (send-signal "{:to \"server:conjure\", :body {:name \"object2\", :type \"Object\"}}")))
    (is (= "I got 'message' from eric:?" (send-signal "{:to \"object2:ping\", :body \"message\"}")))))

(deftest help-test
  (testing "help overview"
    (is (= (str "exit: Terminate connection with the server\n"
                "rl: Request a list of all receptor specification on the server\n"
                "gs: Get state\n"
                "users: Get a list of logged in users\n"
                "ss: Send a signal (new version)\n"
                "send: Send a signal to a receptor.\n"
                "help: Show available commands and what they do.\n"
                "dump: Dump current tree of receptors")
           (help))))
  (testing "help on specific commands"
    (is (= "users: Get a list of logged in users" (help "users"))))
  (testing "help on missing command"
    (is (= "No such command foobar" (help "foobar")))))

(deftest users-test
  (testing "getting a list of users"
    (binding [*server-receptor* (make-server "server")
              *server-state-file-name* "testing-server.state"]
      (let [[server client-stream] (make-client-server)]
        (.write client-stream "eric\n")
        (is (= "[\"eric\"]" (users)))))))

(def-command-test dump-test
  (testing "dump of vanilla server"
    (is (= "#{}" (dump)))
    (send-signal "{:to \"server:conjure\", :body {:name \"object2\", :type \"Object\"}}")
    (is (= "#{{:name- \"object2\", :type- \"Object\", :receptors- #{}}}" (dump)))))

(def-command-test exit-test
  (testing "exiting"
    (send-signal "{:to \"server:conjure\", :body {:name \"object2\", :type \"Object\"}}")
    (is (= "Goodbye eric!"
           (exit))))
  (testing "state saving"
    (is (= (slurp *server-state-file-name*)
           (serialize-receptor *server-receptor*)))))

(def-command-test execute-test
  ;; Silence the error!
  (testing "executing a no argument command"
    (is (= "Goodbye eric!"
           (execute "exit"))))
  (testing "executing a non existent command"
    (is (= "Unknown command: 'fish'. Try help for a list of commands."
           (execute "fish"))))
  (testing "executing a multi argument command"
    (is (= "created"
           (execute "send {:to \"server:conjure\", :body {:name \"object2\", :type \"Object\"}}"))))
  (binding [*err* (java.io.PrintWriter. (writer "/dev/null"))]
    (testing "executing command that throws an error"
      (is (= (execute "send {:to \"zippy:?\", :body \"some body\"}")
             "ERROR: java.lang.RuntimeException: No route to 'zippy:?'"
             )))))

(def-command-test ss-test
  (testing "sending signals"
    (set! *print-level* 10)
    (let [room-addr 
          (ss (json-str {:to 0 :signal "self->host-room" :params {:name "the-room" :password "pass" :matrice-address 33}}))]
      (is (= ["the-room"] (s-> address->resolve (contents *host* :room-scape) room-addr) )))
    ))

(def-command-test rl-test
  (testing "receptor list"
    (signal aspect sig [_r _f])
    (let [result (rl)
          ]
      (is (= (first (vals result)) {"aspect->sig" []})))
    )
  )

(def-command-test gs-test
  (testing "get state"
    (let [host-state (gs "0")
          room-state (gs "4")
          ]
      (is (=  (:type host-state) :host))
      (is (=  (:type room-state) :commons-room))
      )))
