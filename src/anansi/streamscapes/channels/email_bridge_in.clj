(ns
  #^{:author "Eric Harris-Braun"
     :doc "Email Bridge receptor"}
  anansi.streamscapes.channels.email-bridge-in
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]
        [anansi.util :only [date-time-from-java-date standard-date-string]])
  (:use [clj-time.core :only [date-time]]))


(def email-bridge-in-def (receptor-def "email-bridge-in"
                          (attributes :host :account :password :protocol :port)))

(defn convertMultiPartMessage [msg]
  (cond
   (string? msg) msg
   (= (class msg) javax.mail.internet.MimeMultipart)
   (let [c (.getCount msg)]
     (map (fn [i] (let [part (.getBodyPart msg i)
                       type (.getContentType part)
                       content (.getContent part)]
                   {:content-type type :content (if (string? content) content (str "Placeholder for content of type: " (class content)))})) (range c)))
   true (str "Placeholder for content of type: " (class msg))
   ))

(defn parseInternetAddress [ia]
  (if (nil? ia)
    ["_nil1_@unknown.nil","Nil in parse 1"]
    (try (let [f (.getAddress ia)]
           (if (nil? f)
             ["_nil2_@unknown.nil" "Nil in parse 2"]
             [f,(.getPersonal ia)]
             )
           )
         (catch Exception e
           ["_err@unknown.err",(str "Parse err:" e)]))))

(defn handle-message [_r message]
  "process an e-mail: do  look-up to see if we've already created a droplet for this id, and also map the email to/from addresses into identities."
  (let [id (first (.getHeader message "Message-Id"))
        ss (parent-of (parent-of _r))
        ids (get-scape ss :id)
        da (s-> address->resolve ids id)]
    (if (empty? da)
      (let [recipients (try (.getRecipients message javax.mail.Message$RecipientType/TO)
                            (catch Exception e [(javax.mail.internet.InternetAddress. (str "\"" e "\" <_err_@unknown.err>"))])
                            )
            [to to-name] (parseInternetAddress (first recipients))
            [from from-name] (parseInternetAddress
                      (first (try (.getFrom message)
                         (catch Exception e [(javax.mail.internet.InternetAddress. (str "\"" e "\" <_err_@unknown.err>"))]))))
            to-id (do-identify ss {:identifiers {:email to} :attributes {:name to-name}} false)
            from-id (do-identify ss {:identifiers {:email from} :attributes {:name from-name}} false)
            jd (.getSentDate message)
            sent (if (nil? jd) nil (date-time-from-java-date jd))
            subject (.getSubject message)
            ]
        (prn "Handling message with subject: " subject)
        (if (not (nil? subject))
          (--> stream->receive _r (parent-of _r)
               {:id id
                :to to-id
                :from from-id
                :sent (standard-date-string sent)
                :envelope {:from "rfc-822-email" :subject "text/plain" :body (.getContentType message)}
                :content {:from from
                          :subject subject
                          :body (convertMultiPartMessage (.getContent message))}})))
      (first da)
      )))


(defn mail-properties [_r]
  (let [props (java.util.Properties.)]
    (if (= "pop3" (contents _r :protocol ))
      (do
        (.setProperty props "mail.pop3.host" (contents _r :host ))
        (.setProperty props "mail.pop3.port" (contents _r :port ))
        (.setProperty props "mail.pop3.user" (contents _r :account ))
        (if (= (contents _r :port ) 995)
          (.setProperty props "javax.mail.pop3.socketFactory.class"
            "javax.net.ssl.SSLSocketFactory")))
      (.setProperty props "mail.store.protocol", "imaps"))
    props))

(defn pull-messages [_r]
  (let [ props (mail-properties _r)
         session (doto (javax.mail.Session/getInstance props) ;(.setDebug true)
                   )
         store (.getStore session (contents _r :protocol))]
    (prn "HOST ----------> " (contents _r :host))
    (.connect store (contents _r :host ) (contents _r :account) (contents _r :password))
    (let [folder (. store getFolder "Inbox")]
      (println "opening folder")
      (.open folder (javax.mail.Folder/READ_ONLY ))
      (let [message-count (.getMessageCount folder)
            messages (.getMessages folder (- message-count 20) message-count)]
        (println (str "retrieved" (count messages) " messages"))
        (doseq [m messages] (handle-message _r m))
        (.close store)
        ))))


