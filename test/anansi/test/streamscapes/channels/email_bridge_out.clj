(ns anansi.test.streamscapes.channels.email-bridge-out
  (:use [anansi.streamscapes.channels.email-bridge-out] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def stream->send]]
        [anansi.streamscapes.ident :only [ident-def]]
        )
  (:use [midje.sweet])
  (:use [clojure.test])
  (:use [clj-time.core :only [now]]))

(deftest email-bridge-out
  (let [m (make-receptor user-def nil "eric")
        r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor ident-def r {:attributes {:name "Eric"}})
        cc (make-receptor channel-def r {:attributes {:name :email-stream}})
        b (make-receptor email-bridge-out-def cc {:attributes {:host "mail.harris-braun.com" :account "eric@harris-braun.com" :password "some-password" :protocol "smtps" :port 25}})
        ]
    (fact
      (receptor-state b false) => (contains {:host "mail.harris-braun.com" :account "eric@harris-braun.com" :password "some-password" :protocol "smtps" :port 25}))
    (testing "contents"
      (is (= "mail.harris-braun.com" (contents b :host))))
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    (testing "sending mail"
      (is (= (parent-of b) cc))
      (let [
            _ (s-> key->set (get-scape cc :deliverer) :deliverer [(address-of b) channel->deliver])
            i-to (s-> matrice->identify r {:identifiers {:email "lewis.hoffman@gmail.com"} :attributes {:name "Lewis"}})
            i-from (s-> matrice->identify r {:identifiers {:email "eric@harris-braun.com"} :attributes {:name "Eric"}})
            droplet-address (s-> matrice->incorporate r {:to i-to :from i-from :envelope {:subject "text/plain" :body "text/html"} :content {:subject "Hi there!" :body "<b>Hello world!</b>"}})
            ;; two lines bellow commented out to not actually send e-mail
            ; result (s-> stream->send cc {:droplet-address droplet-address })
            ; d (get-receptor r droplet-address)
            deliveries (get-scape r :delivery)
            ]
        (let [[{ aspect :channel time :time}] (s-> address->resolve deliveries droplet-address)]
          ; commented out because e-mail not actually sent
          ; (is (= result "javax.mail.AuthenticationFailedException: 535 Incorrect authentication data\n"))
          (comment is (= channel :email-stream))
          (comment is (= (subs (str (now)) 0 19) (subs time 0 19))) ; hack off the milliseconds
          )))))
