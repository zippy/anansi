(ns anansi.test.streamscapes.channels.email-bridge-in
  (:use [anansi.streamscapes.channels.email-bridge-in] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def]]
        [anansi.streamscapes.ident :only [ident-def]]
        [anansi.receptor.user :only [user-def]])
  (:use [midje.sweet])
  (:use [clojure.test]))

(defn create-java-email-message [{to :to from :from subject :subject body :body}]
  (let [session (doto (javax.mail.Session/getInstance (java.util.Properties.)) (.setDebug false))
        msg (javax.mail.internet.MimeMessage. session)]
    (. msg setFrom (javax.mail.internet.InternetAddress. from))
    (. msg addRecipient javax.mail.Message$RecipientType/TO (javax.mail.internet.InternetAddress. to))
    (. msg setSubject subject)
    (. msg setText body)
    (. msg addHeader "Message-Id" (str "<123456%example.com>"))
    msg)
  )

(deftest email-bridge-in
  (let [m (make-receptor user-def nil "eric")
        r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor ident-def r {:attributes {:name "Eric"}})
        cc (make-receptor channel-def r {:attributes {:name :email-stream}})
        b (make-receptor email-bridge-in-def cc {:attributes {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3"}})
        email-idents (get-scape r :email-ident true)]
    (--> key->set b email-idents "eric@example.com" (address-of eric))

    (fact
      (receptor-state b false) => (contains {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3"}))
    (testing "contents"
      (is (= "mail.example.com" (contents b :host)))
          )

    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    
    (testing "internal functionss: pull-messages"
      ;; testing this requires spoofing an e-mail server for the java mail stuff, so it's not done.
      )
    (testing "internal functions: handle-message"
      (let [message (create-java-email-message {:to "eric@example.com" :from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"})
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (is (= (address-of eric) (contents d :to) ))
        (is (= "<123456%example.com>" (contents d :id)))
        (is (= (s-> key->resolve email-idents "test@example.com")  (contents d :from) ))
        (is (= :email-stream  (contents d :channel) ))
        (is (= {:from "rfc-822-email" :subject "text/plain" :body "text/html"} (contents d :envelope)))
        (is (= {:from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"} (contents d :content)))
        (is (= droplet-address (handle-message b message)))
        )
      ))
)
