(ns anansi.test.streamscapes.bridge-email-in
  (:use [anansi.streamscapes.bridge-email-in] :reload)
  (:use [anansi.streamscapes.channel])
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.streamscapes.streamscapes])
  (:use [clojure.test]))

(defn create-java-email-message [{to :to from :from subject :subject body :body}]
  (let [session (doto (javax.mail.Session/getInstance (java.util.Properties.)) (.setDebug false))
        msg (javax.mail.internet.MimeMessage. session)]
    (. msg setFrom (javax.mail.internet.InternetAddress. from))
    (. msg addRecipient javax.mail.Message$RecipientType/TO (javax.mail.internet.InternetAddress. to))
    (. msg setSubject subject)
    (. msg setText body)
    (. msg addHeader "Message-Id" (str "<"(format "%f" (rand 2)) "%example.com>"))
    msg)
  )

(deftest bridge-email-in
  (let [m (receptor :user nil "eric" nil)
        r (receptor :streamscapes nil (address-of m) "password" {:datax "x"})
        eric (receptor :ident r {:name "Eric"})
        cc (receptor :channel r :email-stream)
        b (receptor :bridge-email-in cc {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3"})
        email-idents (get-scape r :email-ident true)]
    (--> key->set b email-idents "eric@example.com" (address-of eric))
    (testing "contents"
      (is (= "mail.example.com" (contents b :host)))
          )
    (testing "restore"
      (is (=  (state cc true) (state (restore (state cc true) nil) true))))
    (testing "internal functionss: pull-messages"
      ;; testing this requires spoofing an e-mail server for the java mail stuff, so it's not done.
      )
    (testing "internal functions: handle-message"
      (is (= (parent-of b) cc))
      (let [message (create-java-email-message {:to "eric@example.com" :from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"})
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (is (= (address-of eric) (contents d :to) ))
        (is (= (s-> key->resolve email-idents "test@example.com")  (contents d :from) ))
        (is (= :email-stream  (contents d :aspect) ))
        (is (= {:from "rfc-822-email" :subject "text/plain" :body "text/html"} (contents d :envelope)))
        (is (= {:from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"} (contents d :content)))
        (is (= droplet-address (handle-message b message)))
        )
      )
    ))
