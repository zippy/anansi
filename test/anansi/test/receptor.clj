(ns anansi.test.receptor
  (:use [anansi.receptor] :reload)
  (:import anansi.receptor.ObjectReceptor)
  (:import anansi.receptor.Receptor)
  (:use [clojure.test])
  (:use [anansi.test.helpers])
  (:use [anansi.server-constants :only [*server-receptor*]]
        [anansi.server :only [anansi-handle-client]]))

(def my-receptor (ObjectReceptor. "thing"))
(deftest receptor-helpers
  (testing "dumping the contents of a receptor"
    (let [receptor (make-receptor "receptor")]
      (is (= {:name- "receptor", :type- "Receptor", :receptors- #{}} (dump-receptor receptor)))
      (receive receptor {:from "eric:?", :to "receptor:conjure", :body {:name "receptor1",:type "Receptor"}})
      (is (= {:name- "receptor", :type- "Receptor", :receptors- #{{ :name- "receptor1", :type- "Receptor", :receptors- #{}}}} (dump-receptor receptor)))
      (receive receptor {:from "eric:?", :to "receptor:conjure", :body {:name "object2",:type "Object"}})
      (is (= {:name- "receptor", :type- "Receptor", :receptors- #{{ :name- "receptor1", :type- "Receptor", :receptors- #{}}
                                                               { :name- "object2", :type- "Object", :receptors- #{} }}} (dump-receptor receptor)))))
  (testing "dumping the contents of a receptor with attributes"
    (let [eric (make-person "eric" {:eyes "green"})]
      (is (= {:name- "eric", :type- "Person", :attributes { :eyes "green"}, :receptors- #{}} (dump-receptor eric))) ))
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

(deftest receptor-scaping
  (testing "resolving scape keys from a receptor with no scapes"
    (let [receptor (make-receptor "receptor")]
      (is (thrown? RuntimeException (receptor-resolve receptor :some-scape :some-key)))))
  )

(deftest serialzing-receptors
  (testing "serialize unserialize"
    (let [receptor (make-receptor "receptor")]
      (receive receptor {:from "eric:?", :to "receptor:conjure", :body {:name "room1",:type "Room"}})
      (receive receptor {:from "eric:?", :to "room1:enter", :body {:person {:name "Art Brock"}}})
      (receive receptor {:from "eric:?", :to "receptor:conjure", :body {:name "object2",:type "Object"}})
      (is (= (dump-receptor receptor) (dump-receptor (unserialize-receptor (serialize-receptor receptor)))))))
  )

(comment  (deftest host-receptor
            (testing "host receptor saves receptor state"
              (let [host (make-receptor "host")]
                (receive host {:from "from_address:some_aspect", :to "host:conjure", :body {:name "object1",:type "Object"}})
                (save-state host)
                )
              )))

(deftest object-receptor
  (let [my_receptor (ObjectReceptor. "thing")]
    (testing "receiving a valid signal"
      (is (= "I got 'the message' from from_address:some_aspect" (receive my_receptor {:from "from_address:some_aspect", :to "to_address:ping", :body "the message"}))))
    (testing "receiving an invalid signal"
      (is (thrown? RuntimeException (receive my_receptor {:from "from_address:some_aspect", :to "to_address:FISH", :body "the message"}))))
    (testing "getting the aspect list"
      (is (= (get-aspects my_receptor) *base-aspects*))
      )))

(deftest receptor
  (let [receptor (make-receptor "receptor")]
    (testing "receptor aspects"
      (is (= *base-aspects* (get-aspects receptor))))
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
  (let [[server client-stream] (make-client-server)]
    (.write client-stream "eric\n")
    (testing "server aspects"
      (is (= (clojure.set/difference (get-aspects server) *base-aspects*) #{:users}  )))
    (testing "requesting a list of users"
      ;; putting this thread to sleep, it allows the other client
      ;; stream thread to read the write and attach the new user
      (Thread/sleep 10)
      (is (= "[\"eric\"]" (receive server {:from "eric:?", :to "server:users"}))))))

(deftest room-receptor
  (let [room (make-room "room")]
    (testing "room aspects"
      (is (= (clojure.set/difference (get-aspects room) *base-aspects*) #{:describe :enter :leave :scape :pass-object})))
    (testing "person entering and leaving room"
      (is (= "[]" (receive room {:from "eric:?", :to "room:describe"})))
      (is (= "entered as art_brock" (receive room {:from "eric:?", :to "room:enter", :body {:person {:name "Art Brock"}}})))
      (is (= "[\"Art Brock\"]" (receive room {:from "eric:?", :to "room:describe"})))
      (is (= "I got 'the message' from eric:?"
             (receive room {:from "eric:?", :to "art_brock:ping", :body "the message"})))
      (is (= "art_brock left" (receive room {:from "eric:?", :to "room:leave", :body {:person-address "art_brock"}})))
      (is (= "[]" (receive room {:from "eric:?", :to "room:describe"})))
      )
    (testing "serialize room"
      (receive room {:from "eric:?", :to "room:enter", :body {:person {:name "Art Brock"}}})
      (let [room-clone (unserialize-receptor (serialize-receptor room))]
        (is (= (receive room-clone {:to "room:describe"})
               (receive room {:to "room:describe"})))))
    (testing "room scaping"
      (let [room (make-room "room")]
        (is (= #{:seat :angle} (receive room {:from "eric:?", :to "room:scapes"})))
        (receive room {:from "eric:?", :to "room:enter", :body {:person {:name "Art"}}})
        (receive room {:from "eric:?", :to "room:enter", :body {:person {:name "Fernanda"}}})
        (receive room {:from "eric:?", :to "room:enter", :body {:person {:name "Adam"}}})
        (receive room {:from "eric:?", :to "room:enter", :body {:person {:name "Eric"}}})
        (is (= {0 "art", 1 "fernanda", 2 "adam", 3 "eric"} (receptor-scape room :seat)))
        (is (=  {0 "art", 90 "fernanda", 180 "adam", 270 "eric"} (receptor-scape room :angle)))
        (is (= "art" (receive room {:from "eric:?", :to "room:resolve", :body {:scape :seat, :key 0}})))
        (is (= "eric" (receive room {:from "eric:?", :to "room:resolve", :body {:scape :seat, :key 3}})))
        (is (= "art" (receive room {:from "eric:?", :to "room:resolve", :body {:scape :angle, :key 0}})))
        (is (= "adam" (receive room {:from "eric:?", :to "room:resolve", :body {:scape :angle, :key 180}})))
        (receive room {:from "eric:?", :to "room:leave", :body {:person-address "art"}})
        (is (= {0 "fernanda", 1 "adam", 2 "eric"} (receptor-scape room :seat)))
        (is (=  {0 "fernanda", 120 "adam", 240 "eric"} (receptor-scape room :angle)))
        )
      )
    (testing "pasing objects to people in room"
      )
    ))

(deftest person-receptor
  (let [person (make-person "Eric")]
    (testing "person aspects"
      (is (= (clojure.set/difference (get-aspects person) *base-aspects* ) #{:get-attributes :set-attributes :receive-object :release-object})))
    (testing "person Attributes"
      (is (= (receive person {:to "eric:set-attributes", :body {:eyes "blue", :cat "adverb"}})
             {:eyes "blue", :cat "adverb"}))
      (is (= (receive person {:to "eric:get-attributes", :body ""})
             {:eyes "blue", :cat "adverb"}))
      (is (= (receive person {:to "eric:get-attributes", :body {:keys [:eyes]}})
             {:eyes "blue"}))
      )
    (testing "serialize person"
      (let [person-clone (unserialize-receptor (serialize-receptor person))]
        (is (= (receive person-clone {:to "eric:get-attributes", :body {:keys [:eyes]}})
               {:eyes "blue"}))))))
