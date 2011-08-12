
(ns anansi.test.streamscapes.channel
  (:use [anansi.streamscapes.channel] :reload)
  (:use [anansi.ceptr]
        [anansi.streamscapes.streamscapes]
        [anansi.receptor.scape])
  (:use [clojure.test])
  (:use [clj-time.core :only [now]]))

(defmethod manifest :test-send-bridge [_r & args]
           {}
           )

(signal channel deliver [_r _f {droplet-address :droplet-address error :error}]  ;; use the error param to simulate errors or not
        error)

(deftest channel
  (let [m (receptor user nil "eric" nil)
        r (receptor streamscapes nil (address-of m) "password" {:datax "x"})
        cc (receptor channel r :email-stream)]
    (testing "contents"
      (is (= :email-stream (contents cc :name))))

    (testing "receive"
      (let [droplet-address (s-> stream->receive cc {:id "some-id" :to "to-addr" :from "from-addr" :envelope {:from "rfc-822-email" :subject "text/plain" :body "text/html"} :content {:from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"}})
            d (get-receptor r droplet-address)]
        (is (= "from-addr"  (contents d :from) ))
        (is (= "some-id"  (contents d :id) ))
        (is (= :email-stream  (contents d :aspect) ))
        (is (= "to-addr" (contents d :to)))
        (is (= {:from "rfc-822-email" :subject "text/plain" :body "text/html"} (contents d :envelope)))
        (is (= {:from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"} (contents d :content)))))

    (testing "send"
      (let [b (receptor test-send-bridge-email cc {})
            
            _ (s-> key->set (get-scape cc :deliverer) :deliverer [(address-of b) channel->deliver])
            i-to (s-> matrice->identify r {:identifiers {:email "eric@example.com"} :attributes {:name "Eric"}})
            i-from (s-> matrice->identify r {:identifiers {:email "me@example.com"} :attributes {:name "Me"}})
            droplet-address (s-> matrice->incorporate r {:to i-to :from i-from :envelope {:subject "text/plain" :body "text/html"} :content {:subject "Hi there!" :body "<b>Hello world!</b>"}})
            result (s-> stream->send cc {:droplet-address droplet-address :error "Failed"})
            d (get-receptor r droplet-address)
            deliveries (get-scape r :delivery)]
        (is (= "Failed" result))
        (is (= [] (s-> address->resolve deliveries droplet-address)))
        (s-> stream->send cc {:droplet-address droplet-address :error nil})
        (let [[{ aspect :aspect time :time}] (s-> address->resolve deliveries droplet-address)]
          (is (= aspect :email-stream))
          (is (= (subs (str (now)) 0 19) (subs time 0 19))) ; hack off the milliseconds
          )))

    (testing "restore"
      (is (=  (state cc true) (state (restore (state cc true) nil) true))))))
