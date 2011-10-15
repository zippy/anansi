(ns ss.auth
  (:require
   [clojure.string :as string]
   [goog.ui.Dialog :as dialog]
   [goog.ui.Prompt :as prompt]
   [ss.debug :as debug]
   [ss.utils :as u]
   [ss.state :as s]
   [ss.dom-helpers :as d]
   [ss.streamscapes :as sss]
   [ss.ss-utils :as ssu]
   [ss.ceptr :as ceptr]
   [ss.ui :as ui]
   [ss.contact :as contact]
   [ss.compose :as compose]
   ))

(defn do-logged-in [auth-result user-name]
  (do
    (_do-logged-in auth-result user-name)
    (sss/refresh-stream)))

(defn _do-logged-in [auth-result user-name]
  (let [{session :session [sa] :creator} auth-result]
    (s/set-ss-addr sa)
    (s/set-session session)
    (s/set-user-name user-name)
    (let [elem (d/get-element :header-top-right)]
      (d/append elem
                (d/build [:div#logged-in-as (str "Logged in as: " user-name)] )
                (ui/make-button "Logout" do-logged-out)
                (ui/make-button "Contacts" contact/open)
                (ui/make-button "Compose" compose/open)
                ))
    (d/hide :authpane)
    (d/show :container)
    (sss/get-grooves)
    ))

(defn do-logged-out []
  (ui/reset)
  )

(defn auth-callback [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (ui/loading-end)
    (if (= status "ok")
      (do
        (do-logged-in result *auth-user*))
      (do (js/alert result)
          (do-logged-out)))
    (def *auth-user* nil)))

(defn do-auth [] (.setVisible auth true))
(def auth (goog.ui.Prompt. "Authenticate" "User"
                           (fn [r]
                             (if (not ( nil? r))
                               (do
                                 (ui/loading-start)
                                 (do-logged-out)
                                 (def *auth-user* r)
                                 (ceptr/command {:cmd "authenticate" :params {:user r}}  auth-callback))))))


(defn do-new-user [] (.setVisible new-user true))

(defn make-new-user
  "call chain to anansi to create a new user"
  [r]
  (ceptr/start-chain
   {:cleanup  (fn [] (do (sss/refresh-stream)
                        (def *new-user* nil)))
    :error (fn [result] (js/alert (str "Server reported error:" result)))
    :chain [ (fn [result chain] (ceptr/command {:cmd "new-user" :params {:user r}} (ceptr/nextc chain)))
             (fn [result chain] (ceptr/command {:cmd "authenticate" :params {:user *new-user*}} (ceptr/nextc chain)))
             (fn [result chain]
               (s/set-session (:session result))
               (ssu/send-signal {:to 0 :prefix "receptor.host" :aspect "self" :signal "host-streamscape" :params {:matrice-address 999 :name *new-user*}} (ceptr/nextc chain)))
             (fn [sa chain]
               (_do-logged-in {:session (s/get-session) :creator [sa]} *new-user*)
               ;; create the default streamscapes channel for this user
               (ssu/send-ss-signal {:aspect "setup" :signal "new-channel"
                                    :params {:type :streamscapes :name "streamscapes"}})
               ;; as well as a default from identity
               (ssu/send-ss-signal {:aspect "matrice" :signal "identify"
                                    :params {:identifiers {:ss-address sa} :attributes {:name *new-user*}}} (ceptr/nextc chain)))
             (fn [my-contact-addr chain]
               (s/set-me my-contact-addr)
               (ssu/send-ss-signal {:aspect "setup" :signal "new-scape"
                                    :params {:name :sender :relationship {:key "contact-address" :address "boolean"}}} (ceptr/nextc chain)))
             (fn [result chain]
               (ssu/send-ss-signal {:aspect "scape" :signal "set"
                                    :params {:name :sender :key s/*me* :address true}} (ceptr/nextc chain)))
             (fn [result chain]
               (ssu/send-ss-signal {:aspect "setup" :signal "new-scape"
                                    :params {:name "my-scapes" :relationship {:key "scape-name" :address "boolean"}}} (ceptr/nextc chain)))
             ]}))

(def new-user (goog.ui.Prompt. "New User" "User"
                               (fn [r]
                             (if (not ( nil? r))
                               (do
                                 (do-logged-out)
                                 (def *new-user* r)
                                 (make-new-user r)
                                 )))))

(defn check-auth []
  (let [s (s/get-session)]
    (if (or (= s js/undefined) (nil? s)) (do-logged-out) (do-logged-in {:session s :creator [(s/get-ss-addr)]} (s/get-user-name)))))
