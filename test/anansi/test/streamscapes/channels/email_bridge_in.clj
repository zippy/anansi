(ns anansi.test.streamscapes.channels.email-bridge-in
  (:use [anansi.streamscapes.channels.email-bridge-in] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def]]
        [anansi.streamscapes.contact :only [contact-def]]
        [anansi.receptor.user :only [user-def]]
        [anansi.receptor.host :only [host-def]])
  (:use [midje.sweet])
  (:use [clojure.test])
  (:use [clj-time.core :only [date-time]]))

(def *id* 0)
(defn create-java-email-message [{to :to from :from subject :subject body :body sent :sent}]
  (let [session (doto (javax.mail.Session/getInstance (java.util.Properties.)) (.setDebug false))
        to (javax.mail.internet.InternetAddress. to)
        from (javax.mail.internet.InternetAddress. from)
        msg (javax.mail.internet.MimeMessage. session)]
    (. msg setFrom from)
    (. msg addRecipient javax.mail.Message$RecipientType/TO to)
    (. msg setSubject subject)
    (. msg setText body)
    (. msg setSentDate sent)
    (def *id* (+ *id* 1))
    (. msg addHeader "Message-Id" (str "<" *id* "%example.com>"))
    msg)
  )

(deftest email-bridge-in
  (let [h (make-receptor host-def nil {})
        m (make-receptor user-def h "eric")
        r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric-addr (s-> matrice->identify r {:identifiers {:email-address "eric@example.com"} :attributes {:name "Eric"}})
        cc-addr (s-> matrice->make-channel r {:name :email-stream})
        cc (get-receptor r cc-addr)
        b (make-receptor email-bridge-in-def cc {:attributes {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3" :port 110}})
        email-contacts (get-scape r :email-address-contact true)]
    (--> key->set r (get-scape r :channel-type) cc-addr :email)

    (fact
      (receptor-state b false) => (contains {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3" :port 110}))
    
    (testing "contents"
      (is (= "mail.example.com" (contents b :host)))
          )

    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))

;  this test is commented out for regular runs since it actually connects to Google
;  and pulls mail down.
    (comment facts "about pull-messages"
      (let [b2 (make-receptor email-bridge-in-def cc
                              {:attributes {:host "mail.harris-braun.com"
                                            :account "eric@harris-braun.com"
                                            :password "<put a real password here>"
                                            :protocol "pop3"
                                            :port "110"
                                            }})]
        (pull-messages b2)

        )
       (println (str "Receiptscape" (get-scape r :receipt))))


    (testing "internal functions: handle-message"
      (let [sent-date (date-time 2011 01 02 12 21)
            message (create-java-email-message {:sent (java.util.Date. "2011/01/02 12:21") :to "eric@example.com" :from "\"Joe Blow\" <test@example.com>" :subject "Hi there!" :body "<b>Hello world!</b>"})
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            deliveries (get-scape r :delivery)
            ]
        (is (= eric-addr (contents d :to) ))
        (is (= "<1%example.com>" (contents d :id)))
        (is (= (s-> key->resolve email-contacts "test@example.com")  (contents d :from) ))
        (is (= :email-stream  (contents d :channel) ))
        (is (= {:from "rfc-822-email" :subject "text/plain" :body "text/plain"} (contents d :envelope)))
        (is (= {:from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"} (contents d :content)))
        (is (= droplet-address (handle-message b message)))
        (fact (s-> key->resolve (get-scape r :droplet-grooves) droplet-address) => [:subject-body-message])
        (let [[time] (s-> address->resolve deliveries droplet-address)]
          (fact (str sent-date) => time)
          )
        )
      )
    (fact (:scapes (receptor-state r false)) => (contains {:email-address-contact-scape {:values {"eric@example.com" 10, "test@example.com" 14}, :relationship {:key :email-address-identifier, :address :contact-address}
                                                                               }, :contact-name-scape {:values {10 "Eric", 14 "Joe Blow"}, :relationship {:key :contact-address, :address :name-attribute}}}))
    (facts "about content groove scaping (punkmoney)"
      (let [message (create-java-email-message {:sent (java.util.Date. "2011/01/02 12:41") :to "eric@example.com" :from "\"Joe Blow\" <test@example.com>" :subject "Punkmoney Promise" :body "I promise to pay eric@example.com, on demand, some squids. Expires in 1 year."})
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (s-> key->resolve (get-scape r :droplet-grooves) droplet-address) => [:punkmoney :subject-body-message]
        )
      )
    )
)
