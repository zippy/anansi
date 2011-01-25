(ns anansi.test.receptor
  (:use [anansi.receptor] :reload)
  (:import anansi.receptor.SimpleReceptor)
  (:import anansi.receptor.MembraneReceptor)
  (:use [clojure.test]))

(def my-receptor (SimpleReceptor. nil))
(deftest receptor-helpers
  (testing "parsing a signal that's passed in as a string"
    (let  [{:keys [from to body error]} (parse-signal "{:from \"from_address.some_aspect\", :to \"to_address.simple\", :body \"the message\"}")]
      (is (= from {:id "from_address", :aspect :some_aspect}))
      (is (= to {:id "to_address", :aspect :simple}))
      (is (= body "the message"))))
  (testing "parsing the addresses in a signal"
    (let  [{:keys [from to body error]} (parse-signal-addresses {:from "from_address.some_aspect", :to "to_address.simple", :body "the message"})]
      (is (= from {:id "from_address", :aspect :some_aspect}))
      (is (= to {:id "to_address", :aspect :simple}))
      (is (= body "the message"))))
  (testing "parsing an address"
    (let  [{:keys [id aspect]} (parse-address "from_address.some_aspect")]
      (is (= id "from_address"))
      (is (= aspect :some_aspect))))
  (testing "validating a valid signal"
    (let  [{:keys [error]} (validate-signal my-receptor (parse-signal {:from "from_address.some_aspect", :to "to_address.simple", :body "the message"}))]
    (is (= error nil))))
  (testing "validating a valid signal"
    (let  [{:keys [error]} (validate-signal my-receptor (parse-signal {:from "from_address.some_aspect", :to "to_address.simple", :body "the message"}))]
    (is (= error nil))))
  (testing "validating a invalid signal"
    (let  [{:keys [error]} (validate-signal my-receptor (parse-signal {:from "from_address.some_aspect", :to "to_address.FISH", :body "the message"}))]
    (is (= error "unknown aspect :FISH")))))


(deftest simple-recptor
  (let [my_receptor (SimpleReceptor. nil)]
    (testing "receiving a valid signal"
      (is (= "I got 'the message' from: from_address.some_aspect" (receive my_receptor {:from "from_address.some_aspect", :to "to_address.simple", :body "the message"}))))
    (testing "receiving an invalid signal"
      (is (thrown? RuntimeException (receive my_receptor {:from "from_address.some_aspect", :to "to_address.FISH", :body "the message"}))))
    (testing "getting the aspect list"
      (is (= #{:simple} (get-aspects my_receptor))))))

(deftest membrane-recptor
  (let [membrane (create-membrane)]
    (testing "membrane aspects"
      (is (= #{:create} (get-aspects membrane))))
    (testing "sending a message to a non existent receptor"
      (is (thrown? RuntimeException
             (receive membrane {:from "from_address.some_aspect", :to "fish.simple", :body "the message"}))))
    (testing "create simple receptor in a membrane and sending it a signal"
      (is (= "created" (receive membrane {:from "from_address.some_aspect", :to "to_address.create", :body {:name "simple1",:type "Simple"}})))
      (is (= "I got 'the message' from: from_address.some_aspect"
             (receive membrane {:from "from_address.some_aspect", :to "simple1.simple", :body "the message"}))))))
