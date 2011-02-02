(ns anansi.test.receptor
  (:use [anansi.receptor] :reload)
  (:import anansi.receptor.ObjectReceptor)
  (:import anansi.receptor.Receptor)
  (:use [clojure.test])
  (:use [anansi.commands :only [*server-receptor*]]
        [anansi.server :only [anansi-handle-client]]))

(def my-receptor (ObjectReceptor. "thing"))
(deftest receptor-helpers
  (testing "dumping the contents of a receptor"
    (let [receptor (make-receptor "receptor")]
      (is (= {:name "server", :type "Server", :contents #{}} (dump-receptor *server-receptor*)))
      (is (= {:name "receptor", :type "Receptor", :contents #{}} (dump-receptor receptor)))
      (receive receptor {:from "eric:?", :to "receptor:conjure", :body {:name "receptor1",:type "Receptor"}})
      (is (= {:name "receptor", :type "Receptor", :contents #{{ :name "receptor1", :type "Receptor", :contents #{}}}} (dump-receptor receptor)))
      (receive receptor {:from "eric:?", :to "receptor:conjure", :body {:name "object2",:type "Object"}})
      (is (= {:name "receptor", :type "Receptor", :contents #{{ :name "receptor1", :type "Receptor", :contents #{}}
                                           { :name "object2", :type "Object", :contents #{} }}} (dump-receptor receptor)))))
  (testing "parsing a signal that's passed in as a string"
    (let  [{:keys [from to body error]} (parse-signal "{:from \"from_address:some_aspect\", :to \"to_address:ping\", :body \"the message\"}")]
      (is (= from {:id "from_address", :aspect :some_aspect}))
      (is (= to {:id "to_address", :aspect :ping}))
      (is (= body "the message"))))
  (testing "parsing the addresses in a signal"
    (let  [{:keys [from to body error]} (parse-signal-addresses {:from "from_address:some_aspect", :to "to_address:ping", :body "the message"})]
      (is (= from {:id "from_address", :aspect :some_aspect}))
      (is (= to {:id "to_address", :aspect :ping}))
      (is (= body "the message"))))
  (testing "parsing an address"
    (let  [{:keys [id aspect]} (parse-address "from_address:some_aspect")]
      (is (= id "from_address"))
      (is (= aspect :some_aspect))))
  (testing "routing an address"
    (let [scape {:receptors (ref {"ojb1" :object, "memb1" :membrane}), :self "server"}]
      (is (= [:self {:id "server", :aspect :conjure}]) (resolve-address scape {:id "server", :aspect :conjure}))
      (is (= [:object {:id "obj1", :aspect :ping}]) (resolve-address scape {:id "obj1", :aspect :ping}))
      (is (= [:membrane {:id "memb2.obj3" :aspect :ping}]) (resolve-address scape {:id "memb1.memb2.obj3", :aspect :ping}))))
  (testing "validating a valid signal"
    (let  [{:keys [error]} (validate-signal my-receptor (parse-signal {:from "from_address:some_aspect", :to "to_address:ping", :body "the message"}))]
    (is (= error nil))))
  (testing "validating a valid signal"
    (let  [{:keys [error]} (validate-signal my-receptor (parse-signal {:from "from_address:some_aspect", :to "to_address:ping", :body "the message"}))]
    (is (= error nil))))
  (testing "validating a invalid signal"
    (let  [{:keys [error]} (validate-signal my-receptor (parse-signal {:from "from_address:some_aspect", :to "to_address:FISH", :body "the message"}))]
      (is (= error "unknown aspect :FISH"))))
  (testing "humanizing an address"
    (is (= "object:ping" (humanize-address {:id "object", :aspect "ping"})))
    (is (= "object:ping" (humanize-address "object:ping"))))
  (testing "sanitizing a string for use as an adress"
    (is (= "jane_smith" (sanitize-for-address "Jane Smith")))))

(deftest serialzing-receptors
  (testing "serialize unserialize"
    (let [receptor (make-receptor "receptor")]
      (receive receptor {:from "eric:?", :to "receptor:conjure", :body {:name "room1",:type "Room"}})
      (receive receptor {:from "eric:?", :to "room1:enter", :body {:person {:name "Art Brock"}}})
      (receive receptor {:from "eric:?", :to "receptor:conjure", :body {:name "object2",:type "Object"}})
      (is (= (dump-receptor receptor) (dump-receptor (unserialize-receptor (serialize-receptor receptor)))))))
  )


(deftest object-receptor
  (let [my_receptor (ObjectReceptor. "thing")]
    (testing "receiving a valid signal"
      (is (= "I got 'the message' from from_address:some_aspect" (receive my_receptor {:from "from_address:some_aspect", :to "to_address:ping", :body "the message"}))))
    (testing "receiving an invalid signal"
      (is (thrown? RuntimeException (receive my_receptor {:from "from_address:some_aspect", :to "to_address:FISH", :body "the message"}))))
    (testing "getting the aspect list"
      (is (= #{:ping :conjure} (get-aspects my_receptor))))))

(deftest receptor
  (let [receptor (make-receptor "receptor")]
    (testing "receptor aspects"
      (is (= #{:ping :conjure} (get-aspects receptor))))
    (testing "sending a message to a non existent receptor"
      (is (thrown? RuntimeException
                   (receive receptor {:from "from_address:some_aspect", :to "fish:ping", :body "the message"}))))
    (testing "create object receptor in a receptor and sending it a signal"
      (is (= "created" (receive receptor {:from "from_address:some_aspect", :to "receptor:conjure", :body {:name "object1",:type "Object"}})))
      (is (= "I got 'the message' from from_address:some_aspect"
             (receive receptor {:from "from_address:some_aspect", :to "object1:ping", :body "the message"}))))
    (testing "create an uknown receptor type"
      (is (thrown? RuntimeException
                   (receive receptor {:from "from_address:some_aspect", :to "receptor:conjure", :body {:name "fish1",:type "Fish"}}))))
    (testing "create a receptor inside the receptor, an object inside it and send it a message"
      (is (= "created" (receive receptor {:from "eric:?", :to "receptor:conjure", :body {:name "receptor1",:type "Receptor"}})))
      (is (= "created" (receive receptor {:from "eric:?", :to "receptor1:conjure", :body {:name "object2",:type "Object"}})))
      (is (= "I got 'the message' from eric:?"
             (receive receptor {:from "eric:?", :to "receptor1.object2:ping", :body "the message"}))))))

(deftest server-receptor
  (let [server (make-server "server")
        client-stream (java.io.PipedWriter.)
        r (java.io.BufferedReader. (java.io.PipedReader. client-stream))]
    (doto (Thread. #(do (anansi-handle-client r *out*))) .start)
    (.write client-stream "eric\n")
    (testing "server aspects"
      (is (= #{:ping :conjure :users} (get-aspects server))))
    (testing "requesting a list of users"
      ;; putting this thread to sleep, it allows the other client
      ;; stream thread to read the write and attach the new user
      (Thread/sleep 10)
      (is (= "[\"eric\"]" (receive server {:from "eric:?", :to "server:users"}))))))

(deftest room-receptor
  (let [room (make-room "room")]
    (testing "room aspects"
      (is (= #{:ping :conjure :describe :enter :leave :scape :pass-object} (get-aspects room))))
    (testing "person entering and leaving room"
      (is (= "[]" (receive room {:from "eric:?", :to "room:describe"})))
      (is (= "entered as art_brock" (receive room {:from "eric:?", :to "room:enter", :body {:person {:name "Art Brock"}}})))
      (is (= "[\"Art Brock\"]" (receive room {:from "eric:?", :to "room:describe"})))
      (is (= "I got 'the message' from eric:?"
             (receive room {:from "eric:?", :to "art_brock:ping", :body "the message"})))
      (is (= "art_brock left" (receive room {:from "eric:?", :to "room:leave", :body {:person-address "art_brock"}})))
      (is (= "[]" (receive room {:from "eric:?", :to "room:describe"})))
      )
    (testing "pasing objects to people in room"
      )
    ))

(deftest person-receptor
  (let [person (make-person "Eric")]
    (testing "person aspects"
      (is (= #{:ping :conjure :get-attributes :set-attributes :receive-object :release-object} (get-aspects person))))
    (testing "person Attributes"
      (is (= (receive person {:to "eric:set-attributes", :body {:eyes "blue", :cat "adverb"}})
             {:eyes "blue", :cat "adverb"}))
      (is (= (receive person {:to "eric:get-attributes", :body ""})
             {:eyes "blue", :cat "adverb"}))
      (is (= (receive person {:to "eric:get-attributes", :body {:keys [:eyes]}})
             {:eyes "blue"}))
      )
    (comment "this is pending understanding the relationship between sub-receptors and receptor data and scaping"
             (testing "serialize person"
               (let [person-clone (unserialize-receptor (serialize-receptor person))]
                 (is (= (receive person-clone {:to "eric:get-attributes", :body {:keys [:eyes]}})
                        {:eyes "blue"})))
               ))))
