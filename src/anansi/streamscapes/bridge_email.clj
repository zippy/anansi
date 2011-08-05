(ns
  #^{:author "Eric Harris-Braun"
     :doc "Email Bridge receptor"}
  anansi.streamscapes.bridge-email
  (:use [anansi.ceptr]
        [anansi.streamscapes.streamscapes]
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

(defn handle-message [_r message]
  (--> stream->receive _r (parent-of _r)
       {:to "to-addr"
        :envelope {:from "rfc-822-email" :subject "text/plain" :body "text/html"}
        :content {:from (javax.mail.internet.InternetAddress/toString (.getFrom message))
                  :subject (.getSubject message)
                  :body (.getContent message)}})
  )

