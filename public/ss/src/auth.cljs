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
   [ss.ident :as ident]
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
      (d/append elem (d/build [:div#logged-in-as (str "Logged in as: " user-name)] ) (ui/make-button "Logout" do-logged-out) (ui/make-button "Contacts" ident/open)))
    (d/hide :authpane)
    (d/show :container)
    ))

(defn do-logged-out []
  (do
    (d/remove-children :the-receptor)
    (d/remove-children :debug)
    (d/remove-children :header-top-right)
    (s/clear-session)
    (d/hide :container)
    (d/show :authpane))
  )

(defn first-make-ss-callback [e]
  (let [{status :status sa :result} (ceptr/handle-xhr e)]
    (if (= status "ok")
      (do (_do-logged-in {:session (s/get-session) :creator [sa]} *new-user*)
          ;; create the default streamscapes channel for this user
          (ssu/send-ss-signal {:aspect "setup" :signal "new-channel"
                               :params {:type :streamscapes :name "streamscapes"}})
          ;; as well as a default from identity
          (ssu/send-ss-signal {:aspect "matrice" :signal "identify"
                            :params {:identifiers {:ss-address sa} :attributes {:name *new-user*}}} sss/refresh-stream-callback))
      )
    (def *new-user* nil)))

(defn first-auth-callback [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (if (= status "ok")
      (do
        (s/set-session (:session result))
        (ssu/send-signal {:to 0 :prefix "receptor.host" :aspect "self" :signal "host-streamscape" :params {:matrice-address 999 :name *new-user*}} first-make-ss-callback))
      (def *new-user* nil))))

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

(defn new-user-callback [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (if (= status "ok")
      (do
        (ceptr/command {:cmd "authenticate" :params {:user *new-user*}} first-auth-callback))
      (do
        (def *new-user* nil)
        (js/alert result)))))

(defn do-new-user [] (.setVisible new-user true))
(def new-user (goog.ui.Prompt. "New User" "User"
                               (fn [r]
                             (if (not ( nil? r))
                               (do
                                 (do-logged-out)
                                 (def *new-user* r)
                                 (ceptr/command {:cmd "new-user" :params {:user r}}  new-user-callback))))))

(defn check-auth []
  (let [s (s/get-session)]
    (if (or (= s js/undefined) (nil? s)) (do-auth) (do-logged-in {:session s :creator [(s/get-ss-addr)]} (s/get-user-name)))))
