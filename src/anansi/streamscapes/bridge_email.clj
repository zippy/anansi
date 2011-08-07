(ns
  #^{:author "Eric Harris-Braun"
     :doc "Email Bridge receptor"}
  anansi.streamscapes.bridge-email
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.ident]
        [anansi.streamscapes.channel]))

(defmethod manifest :bridge-email [_r {host :host account :account password :password protocol :protocol}]
           {:host host :account account :password password :protocol protocol})
(defmethod state :bridge-email [_r full?]
           (assoc (state-convert _r full?)
             :host (contents _r :host)
             :account (contents _r :account)
             :password (contents _r :password)
             :protocol (contents _r :protocol)
             ))
(defmethod restore :bridge-email [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :host (:host state))
             (restore-content r :account (:account state))
             (restore-content r :password (:password state))
             (restore-content r :protocol (:protocol state))
             r))

(defn resolve-or-create-email-ident [_r email-ids email-address]
  (or (--> key->resolve _r email-ids email-address)
      (let [ident (receptor ident _r {:name (str "name for " email-address)})
            ident-address (address-of ident)]
        (--> key->set _r email-ids email-address ident-address)
        ident-address)) ;; see if we have the name in the java email object
  )

(defn handle-message [_r message]
  (let [id (first (.getHeader message "Message-Id"))
        ss (parent-of (parent-of _r))
        ids (contents ss :id-scape)
        email-ids (contents ss :email-id-scape)
        da (s-> address->resolve ids id)]
    (if (empty? da)
      (let [to (.toString (first (.getRecipients message javax.mail.Message$RecipientType/TO)))
            from (javax.mail.internet.InternetAddress/toString (.getFrom message))
            to-id (resolve-or-create-email-ident ss email-ids to)
            from-id (resolve-or-create-email-ident ss email-ids from)]
        (--> stream->receive _r (parent-of _r)
             {:id id
              :to to-id
              :from from-id
              :envelope {:from "rfc-822-email" :subject "text/plain" :body "text/html"}
              :content {:from from
                        :subject (.getSubject message)
                        :body (.getContent message)}}))
      (first da)
      )
    )
  
  )

(defn pull-messages [_r]
  (let [props (java.util.Properties.)
        session (doto (javax.mail.Session/getInstance (java.util.Properties.)) (.setDebug false))
        store (.getStore session (contents _r :protocol))]
    (.connect store (contents _r :host) (contents _r :account) (contents _r :password))
    (let [folder (. store getFolder "inbox")]
      (.open folder (javax.mail.Folder/READ_ONLY ))
      (let [messages (.getMessages folder)]
        (for [m messages] (handle-message _r m))
        (.close store)
        ))))

