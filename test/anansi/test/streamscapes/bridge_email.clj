(ns anansi.test.streamscapes.bridge-email
  (:use [anansi.streamscapes.bridge-email] :reload)
  (:use [anansi.streamscapes.channel])
  (:use [anansi.ceptr])
  (:use [anansi.streamscapes.streamscapes])
  (:use [clojure.test]))

(defn create-java-email-message [{to :to from :from subject :subject body :body}]
  (let [session (doto (javax.mail.Session/getInstance (java.util.Properties.)) (.setDebug false))
        msg (javax.mail.internet.MimeMessage. session)]
    (. msg setFrom (javax.mail.internet.InternetAddress. from))
    (. msg addRecipients javax.mail.Message$RecipientType/TO to)
    (. msg setSubject subject)
    (. msg setText body)
    msg)
  )

(deftest bridge-email
  (let [m (receptor user nil "eric" nil)
        r (receptor streamscapes nil (address-of m) "password" {:datax "x"})
        cc (receptor channel r :email-stream)
        b (receptor bridge-email cc {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3"})]
    (testing "contents"
      (is (= "mail.example.com" (contents b :host)))
          )
    (testing "restore"
      (is (=  (state cc true) (state (restore (state cc true) nil) true))))
    (testing "internal functions: handle-message"
      (is (= (parent-of b) cc))
      (let [message (create-java-email-message {:to "dest@example.com" :from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"})
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (is (= (address-of cc)  (contents d :from) ))
        (is (= :email-stream  (contents d :aspect) ))
        (is (= "to-addr" (contents d :to)))
        (is (= {:from "rfc-822-email" :subject "text/plain" :body "text/html"} (contents d :envelope)))
        (is (= {:from "test@example.com" :subject "Hi there!" :body "<b>Hello world!</b>"} (contents d :content)))
        )
      )
    ))
