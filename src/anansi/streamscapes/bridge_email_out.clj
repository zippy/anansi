(ns
  #^{:author "Eric Harris-Braun"
     :doc "Email Bridge receptor"}
  anansi.streamscapes.bridge-email-out
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]))

(defmethod manifest :bridge-email-out [_r {host :host account :account password :password protocol :protocol port :port}]
           {:host host :account account :password password :protocol protocol :port port})

(defmethod state :bridge-email-out [_r full?]
           (assoc (state-convert _r full?)
             :host (contents _r :host)
             :account (contents _r :account)
             :password (contents _r :password)
             :protocol (contents _r :protocol)
             :port (contents _r :port)
             ))

(defmethod restore :bridge-email-out [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :host (:host state))
             (restore-content r :account (:account state))
             (restore-content r :password (:password state))
             (restore-content r :protocol (:protocol state))
             (restore-content r :port (:port state))
             r))

(signal channel deliver [_r _f {droplet-address :droplet-address}] 
        (let [
              props (java.util.Properties.)
              ss (parent-of  (parent-of _r))
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
                  email-idents (get-scape ss :email-ident)
                  to-email (first (--> address->resolve _r email-idents (contents d :to)))
                  from-email (first (--> address->resolve _r email-idents (contents d :from)))
                  content (contents d :content)
                  envelope (contents d :envelope)
                  ]
              (. msg setFrom (javax.mail.internet.InternetAddress. from-email))
              (. msg addRecipients javax.mail.Message$RecipientType/TO to-email)
          
              (. msg setSubject (:subject content))
              (. msg setText (:body content))
          
              (. msg setHeader "X-Mailer", "msgsend")
              (. msg setHeader "X-Streamscapes-Droplet-Address", (str (address-of ss) "." droplet-address))
              (. msg setSentDate (java.util.Date.))
            
              (javax.mail.Transport/send msg)
              nil)
            (catch Exception e
              (str e)))
          ))
