(ns anansi.test.receptor
  (:use [anansi.receptor] :reload)
  (:import anansi.receptor.ObjectReceptor)
  (:import anansi.receptor.MembraneReceptor)
  (:use [clojure.test]))

(def my-receptor (ObjectReceptor. "thing"))
(deftest receptor-helpers
  (testing "parsing a signal that's passed in as a string"
    (let  [{:keys [from to body error]} (parse-signal "{:from \"from_address.some_aspect\", :to \"to_address.ping\", :body \"the message\"}")]
      (is (= from {:id "from_address", :aspect :some_aspect}))
      (is (= to {:id "to_address", :aspect :ping}))
      (is (= body "the message"))))
  (testing "parsing the addresses in a signal"
    (let  [{:keys [from to body error]} (parse-signal-addresses {:from "from_address.some_aspect", :to "to_address.ping", :body "the message"})]
      (is (= from {:id "from_address", :aspect :some_aspect}))
      (is (= to {:id "to_address", :aspect :ping}))
      (is (= body "the message"))))
  (testing "parsing an address"
    (let  [{:keys [id aspect]} (parse-address "from_address.some_aspect")]
      (is (= id "from_address"))
      (is (= aspect :some_aspect))))
  (testing "validating a valid signal"
    (let  [{:keys [error]} (validate-signal my-receptor (parse-signal {:from "from_address.some_aspect", :to "to_address.ping", :body "the message"}))]
    (is (= error nil))))
  (testing "validating a valid signal"
    (let  [{:keys [error]} (validate-signal my-receptor (parse-signal {:from "from_address.some_aspect", :to "to_address.ping", :body "the message"}))]
    (is (= error nil))))
  (testing "validating a invalid signal"
    (let  [{:keys [error]} (validate-signal my-receptor (parse-signal {:from "from_address.some_aspect", :to "to_address.FISH", :body "the message"}))]
    (is (= error "unknown aspect :FISH")))))


(deftest object-recptor
  (let [my_receptor (ObjectReceptor. "thing")]
    (testing "receiving a valid signal"
      (is (= "I got 'the message' from: from_address.some_aspect" (receive my_receptor {:from "from_address.some_aspect", :to "to_address.ping", :body "the message"}))))
    (testing "receiving an invalid signal"
      (is (thrown? RuntimeException (receive my_receptor {:from "from_address.some_aspect", :to "to_address.FISH", :body "the message"}))))
    (testing "getting the aspect list"
      (is (= #{:ping} (get-aspects my_receptor))))))

(deftest membrane-recptor
  (let [membrane (create-membrane)]
    (testing "membrane aspects"
      (is (= #{:conjure} (get-aspects membrane))))
    (testing "sending a message to a non existent receptor"
      (is (thrown? RuntimeException
             (receive membrane {:from "from_address.some_aspect", :to "fish.ping", :body "the message"}))))
    (testing "create object receptor in a membrane and sending it a signal"
      (is (= "created" (receive membrane {:from "from_address.some_aspect", :to "to_address.conjure", :body {:name "object1",:type "Object"}})))
      (is (= "I got 'the message' from: from_address.some_aspect"
             (receive membrane {:from "from_address.some_aspect", :to "object1.ping", :body "the message"}))))))

(deftest person-recptor
  (let [person (create-person "Eric")]
    (testing "person aspects"
      (is (= #{:get-attributes :set-attributes :receive-object :release-object} (get-aspects person))))
    (testing "person Attributes"
      (is (= (receive person {:to "eric.set-attributes", :body {:eyes "blue", :cat "adverb"}})
             {:eyes "blue", :cat "adverb"}))
      (is (= (receive person {:to "eric.get-attributes", :body ""})
             {:eyes "blue", :cat "adverb"}))
      (is (= (receive person {:to "eric.get-attributes", :body {:keys [:eyes]}})
             {:eyes "blue"}))
      )
    ))
