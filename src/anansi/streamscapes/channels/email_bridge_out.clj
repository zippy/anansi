(ns
  #^{:author "Eric Harris-Braun"
     :doc "Email Bridge receptor"}
  anansi.streamscapes.channels.email-bridge-out
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]))

(def email-bridge-out-def (receptor-def "email-bridge-out"
                          (attributes :host :account :password :protocol :port)))

(signal channel deliver [_r _f {droplet-address :droplet-address}] 
        (let [
              props (java.util.Properties.)
              cc (parent-of _r)
              ss (parent-of  cc)
              d (get-receptor ss droplet-address)
              ]

          (. props put "mail.smtp.host", (contents _r :host))
          (. props put "mail.smtp.port", (contents _r :port))
          (. props put "mail.smtp.auth", "true")
          (. props put "mail.transport.protocol", (contents _r :protocol))
          
          (try
            (let [
                  authenticator (proxy [javax.mail.Authenticator] [] 
                                  (getPasswordAuthentication 
                                   []
                                   (javax.mail.PasswordAuthentication. 
                                    (contents _r :account) (contents _r :password))))
                  session (javax.mail.Session/getDefaultInstance props authenticator)
                  msg (javax.mail.internet.MimeMessage. session)
                  email-contacts (get-scape ss :email-contact)
                  to-email (first (--> address->resolve _r email-contacts (contents d :to)))
                  from-email (first (--> address->resolve _r email-contacts (contents d :from)))
                  content (contents d :content)
                  envelope (contents d :envelope)
                  ]
              (. msg setFrom (javax.mail.internet.InternetAddress. from-email))
              (. msg addRecipients javax.mail.Message$RecipientType/TO to-email)
          
              (. msg setSubject (:subject content))
              (. msg setText (:body content))
              
              (. msg setHeader "Content-Type", (:body envelope))
              (. msg setHeader "X-Mailer", (str "Streamscapes-channel: " (name (contents cc :name))))
              (. msg setHeader "X-Streamscapes-Droplet-Address", (str (address-of ss) "." droplet-address))
              (. msg setSentDate (java.util.Date.))
            
              (javax.mail.Transport/send msg)
              nil)
            (catch Exception e
              (str e)))
          ))
