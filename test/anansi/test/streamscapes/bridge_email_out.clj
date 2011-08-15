(ns anansi.test.streamscapes.bridge-email-out
  (:use [anansi.streamscapes.bridge-email-out] :reload)
  (:use [anansi.streamscapes.channel])
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.streamscapes.streamscapes])
  (:use [clojure.test])
  (:use [clj-time.core :only [now]]))

(deftest bridge-email-out
  (let [m (receptor :user nil "eric" nil)
        r (receptor :streamscapes nil (address-of m) "password" {:datax "x"})
        eric (receptor :ident r {:name "Eric"})
        cc (receptor :channel r :email-stream)
        b (receptor :bridge-email-out cc {:host "mail.harris-braun.com" :account "eric@harris-braun.com" :password "some-password" :protocol "smtps" :port 25})
        ]
    
    (testing "contents"
      (is (= "mail.harris-braun.com" (contents b :host))))
    (testing "restore"
      (is (=  (state cc true) (state (restore (state cc true) nil) true))))
    (testing "sending mail"
      (is (= (parent-of b) cc))
      (let [
            _ (s-> key->set (get-scape cc :deliverer) :deliverer [(address-of b) channel->deliver])
            i-to (s-> matrice->identify r {:identifiers {:email "lewis.hoffman@gmail.com"} :attributes {:name "Lewis"}})
            i-from (s-> matrice->identify r {:identifiers {:email "eric@harris-braun.com"} :attributes {:name "Eric"}})
            droplet-address (s-> matrice->incorporate r {:to i-to :from i-from :envelope {:subject "text/plain" :body "text/html"} :content {:subject "Hi there!" :body "<b>Hello world!</b>"}})
            result (s-> stream->send cc {:droplet-address droplet-address })
            ;;            d (get-receptor r droplet-address)
            deliveries (get-scape r :delivery)
            ]
        (let [[{ aspect :aspect time :time}] (s-> address->resolve deliveries droplet-address)]
          (is (= result "javax.mail.AuthenticationFailedException: 535 Incorrect authentication data\n"))
          (comment is (= aspect :email-stream))
          (comment is (= (subs (str (now)) 0 19) (subs time 0 19))) ; hack off the milliseconds
          )
        )
      )
    ))
