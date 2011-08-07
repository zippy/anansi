
(ns anansi.test.streamscapes.channel
  (:use [anansi.streamscapes.channel] :reload)
  (:use [anansi.ceptr])
  (:use [anansi.streamscapes.streamscapes])
  (:use [clojure.test]))

(deftest channel
  (let [m (receptor user nil "eric" nil)
        r (receptor streamscapes nil (address-of m) "password" {:datax "x"})
        cc (receptor channel r :email-stream)]
    (testing "contents"
      (is (= :email-stream (contents cc :name)))
      )
    (testing "receive"
      (let [droplet-address (s-> stream->receive cc {:id "some-id" :to "to-addr" :from "from-addr" :envelope {:from "rfc-822-email" :subject "text/plain" :body "text/html"} :content {:from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"}})
            d (get-receptor r droplet-address)]
        (is (= "from-addr"  (contents d :from) ))
        (is (= "some-id"  (contents d :id) ))
        (is (= :email-stream  (contents d :aspect) ))
        (is (= "to-addr" (contents d :to)))
        (is (= {:from "rfc-822-email" :subject "text/plain" :body "text/html"} (contents d :envelope)))
        (is (= {:from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"} (contents d :content)))))
    (testing "restore"
      (is (=  (state cc true) (state (restore (state cc true) nil) true))))
    ))
