(ns anansi.test.commands
  (:use [anansi.ceptr]
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

(deftest help-test
  (testing "help overview"
    (is (= java.lang.String (class
                             ;; too much of a pain to change the
                             ;; text each time I change a command...
                             (help)))))
  (testing "help on specific commands"
    (is (= "users: Get a list of logged in users" (help "users"))))
  (testing "help on missing command"
    (is (= "No such command foobar" (help "foobar")))))

(deftest users-test
  (testing "getting a list of users"
    (binding [*server-state-file-name* "testing-server.state"]
      (let [[client-stream] (make-client-server)]
        (.write client-stream "eric\n")
        (is (= ["eric"] (users)))))))

(def-command-test exit-test
  (testing "exiting"
    (is (= "Goodbye eric!"
           (exit))))
  (testing "state saving"
    (is (= (slurp *server-state-file-name*)
           (.toString (serialize-receptors *receptors*))))))

(def-command-test execute-test
  ;; Silence the error!
  (testing "executing a no argument command"
    (is (= {:status :ok, :result "Goodbye eric!"}
           (execute "exit"))))
  (testing "executing a non existent command"
    (is (= {:status :error, :result "Unknown command: 'fish'", :comment "Try 'help' for a list of commands."}
           (execute "fish"))))
  (binding [*err* (java.io.PrintWriter. (writer "/dev/null"))]
    (testing "executing command that throws an error"
      (is (= (execute (str "ss " (json-str  {:to 99 :signal "self->host-room" :params {}})))
             {:status :error, :result "exception raised: java.lang.NullPointerException"}
             
             )))))

(def-command-test ss-test
  (testing "sending signals"
    (set! *print-level* 10)
    (let [room-addr 
          (ss (json-str {:to 0 :signal "self->host-room" :params {:name "the-room" :password "pass" :matrice-address 33}}))]
      (is (= ["the-room"] (s-> address->resolve (get-scape (get-host) :room) room-addr) )))
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
    (is (=  (:type (gs (json-str {:addr 0}))) :host))
    (is (=  (:type (gs)) :host))
    ))

(def-command-test gc-test
  (testing "get count"
    (is (= (gc) @*changes*))
    (let [room-addr 
          (ss (json-str {:to 0 :signal "self->host-room" :params {:name "the-room" :password "pass" :matrice-address 33}}))]
      (comment is (= (gc (json-str {:addr room-addr})) (:changes @(get-receptor (get-host) room-addr)))))))

(def-command-test sp-test
  (testing "set prompt"
    (is (= @*prompt* nil))
    (sp (json-str ">"))
    (is (= @*prompt* ">"))
    (sp "null")
    (is (= @*prompt* nil))
))
