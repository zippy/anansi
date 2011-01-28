(ns anansi.test.receptor
  (:use [anansi.receptor] :reload)
  (:import anansi.receptor.ObjectReceptor)
  (:import anansi.receptor.MembraneReceptor)
  (:use [clojure.test]))

(def my-receptor (ObjectReceptor. "thing"))
(deftest receptor-helpers
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
      (is (= [:membrane {:id "memb2.obj3" :aspect :ping}]) (resolve-address scape {:id "memb1.memb2.obj3", :aspect :ping})))
    )
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
    (is (= "object:ping" (humanize-address "object:ping")))))


(deftest object-recptor
  (let [my_receptor (ObjectReceptor. "thing")]
    (testing "receiving a valid signal"
      (is (= "I got 'the message' from from_address:some_aspect" (receive my_receptor {:from "from_address:some_aspect", :to "to_address:ping", :body "the message"}))))
    (testing "receiving an invalid signal"
      (is (thrown? RuntimeException (receive my_receptor {:from "from_address:some_aspect", :to "to_address:FISH", :body "the message"}))))
    (testing "getting the aspect list"
      (is (= #{:ping} (get-aspects my_receptor))))))

(deftest membrane-recptor
  (let [membrane (make-membrane "membrane")]
    (testing "membrane aspects"
      (is (= #{:conjure} (get-aspects membrane))))
    (testing "sending a message to a non existent receptor"
      (is (thrown? RuntimeException
                   (receive membrane {:from "from_address:some_aspect", :to "fish:ping", :body "the message"}))))
    (testing "create object receptor in a membrane and sending it a signal"
      (is (= "created" (receive membrane {:from "from_address:some_aspect", :to "membrane:conjure", :body {:name "object1",:type "Object"}})))
      (is (= "I got 'the message' from from_address:some_aspect"
             (receive membrane {:from "from_address:some_aspect", :to "object1:ping", :body "the message"}))))
    (testing "create an uknown receptor type"
      (is (thrown? RuntimeException
                   (receive membrane {:from "from_address:some_aspect", :to "membrane:conjure", :body {:name "fish1",:type "Fish"}}))))
    (testing "create a membrane inside the membrane, an object inside it and send it a message"
      (is (= "created" (receive membrane {:from "eric:?", :to "membrane:conjure", :body {:name "membrane1",:type "Membrane"}})))
      (is (= "created" (receive membrane {:from "eric:?", :to "membrane1:conjure", :body {:name "object2",:type "Object"}})))
      (is (= "I got 'the message' from eric:?"
             (receive membrane {:from "eric:?", :to "membrane1.object2:ping", :body "the message"}))))))

(deftest server-recptor
  (let [server (make-server "server")]
    (testing "server aspects"
      (is (= #{:conjure} (get-aspects server))))
    ))

(deftest room-recptor
  (let [room (make-room "room")]
    (testing "room aspects"
      (is (= #{:conjure :describe :enter :leave :scape :pass-object} (get-aspects room))))
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

(deftest person-recptor
  (let [person (make-person "Eric")]
    (testing "person aspects"
      (is (= #{:ping :get-attributes :set-attributes :receive-object :release-object} (get-aspects person))))
    (testing "person Attributes"
      (is (= (receive person {:to "eric:set-attributes", :body {:eyes "blue", :cat "adverb"}})
             {:eyes "blue", :cat "adverb"}))
      (is (= (receive person {:to "eric:get-attributes", :body ""})
             {:eyes "blue", :cat "adverb"}))
      (is (= (receive person {:to "eric:get-attributes", :body {:keys [:eyes]}})
             {:eyes "blue"}))
      )))
