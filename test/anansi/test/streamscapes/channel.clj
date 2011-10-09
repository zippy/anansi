
(ns anansi.test.streamscapes.channel
  (:use [anansi.streamscapes.channel] :reload)
  (:use [anansi.ceptr]
        [anansi.streamscapes.streamscapes]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]])
  (:use [midje.sweet])
  (:use [clojure.test])
  (:use [clj-time.core :only [now]]))

(defmethod manifest :test-send-bridge [_r & args]
           {}
           )

(signal channel deliver [_r _f {droplet-address :droplet-address error :error}]  ;; use the error param to simulate errors or not
        error)

(deftest channel
  (let [m (make-receptor user-def nil "eric")
        r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        cc-addr (s-> matrice->make-channel r {:name :email-stream})
        cc (get-receptor r cc-addr)]
    (fact (receptor-state cc false) => (contains {:name :email-stream
                                                  :fingerprint :anansi.streamscapes.channel.channel
                                                  :scapes {:controller-scape {:values {}, :relationship {:key nil, :address nil}}, :deliverer-scape {:values {}, :relationship {:key nil, :address nil}}, :receiver-scape {:values {}, :relationship {:key nil, :address nil}}}
                                                  }))

    (testing "receive"
      (let [sent-date (str (now))
            droplet-address (s-> stream->receive cc {:id "some-id" :to "to-addr" :from "from-addr" :sent sent-date :envelope {:from "rfc-822-email" :subject "text/plain" :body "text/html"} :content {:from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"}})
            d (get-receptor r droplet-address)
            receipts (get-scape r :receipt)
            deliveries (get-scape r :delivery)]
        (fact (receptor-state d true) => (contains {:id "some-id", :envelope {:from "rfc-822-email", :subject "text/plain", :body "text/html"}, :channel :email-stream, :content {:from "test@example.com", :subject "Hi there!", :body "<b>Hello world!</b>"}, :to "to-addr", :from "from-addr", :fingerprint :anansi.streamscapes.droplet.droplet}))
        (let [[time] (s-> address->resolve receipts droplet-address)]
          (fact (= time sent-date) => false)
          (fact (subs (str (now)) 0 19) => (subs time 0 19)) ;hack off the milliseconds
          )
        (let [[time] (s-> address->resolve deliveries droplet-address)]
          (facts time => sent-date))

        (is (= "from-addr"  (contents d :from) ))
        (is (= "some-id"  (contents d :id) ))
        (is (= :email-stream  (contents d :channel) ))
        (is (= "to-addr" (contents d :to)))
        (is (= {:from "rfc-822-email" :subject "text/plain" :body "text/html"} (contents d :envelope)))
        (is (= {:from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"} (contents d :content)))))

    (testing "send"
      (let [b (make-receptor (receptor-def "test-send-bridge-email") cc {})
            
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
        (let [[time] (s-> address->resolve deliveries droplet-address)]
          (is (= (subs (str (now)) 0 19) (subs time 0 19))) ; hack off the milliseconds
          )))
    
    (facts "about restoring serialized channel receptor"
      (let [channel-state (receptor-state cc true)]
        channel-state => (receptor-state (receptor-restore channel-state nil) true)
        )
      )
    (testing "restore"
      (is (=  (receptor-state cc true) (receptor-state (receptor-restore (receptor-state cc true) nil) true))))))
