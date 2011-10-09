(ns ss.auth
  (:require
   [clojure.string :as string]
   [goog.ui.Dialog :as dialog]
   [goog.ui.Prompt :as prompt]
   [ss.debug :as debug]
   [ss.utils :as u]
   [ss.session :as s]
   [ss.dom-helpers :as d]
   [ss.streamscapes :as sss]
   [ss.ss-utils :as ssu]
   [ss.ceptr :as ceptr]
   [ss.ui :as ui]
   ))

(defn do-logged-in [auth-result]
  (do
    (_do-logged-in auth-result)
    (sss/refresh-stream)))

(defn _do-logged-in [auth-result]
  (let [{session :session [sa] :creator} auth-result]
    (s/set-ss-addr sa)
    (s/set-session session)
    (def ss-addr sa)
    (d/hide :authpane)
    (d/show :container)
    ))

(defn do-logged-out []
  (do
    (d/remove-children :the-receptor)
    (d/remove-children :debug)
    (s/clear-session)
    (d/hide :container)
    (d/show :authpane))
  )

(defn make-ss-channel [sa]
  (ssu/send-signal {:to sa :prefix "streamscapes.streamscapes" :aspect "setup" :signal "new-channel"
                    :params {:type :streamscapes :name "streamscapes"}}))

;; TODO this is currently duplicated with a callback in core.  It
;; needs to be refactored into a single file
(defn refresh-stream-callback [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (sss/refresh-stream)
    )
  )
(defn first-make-ss-callback [e]
  (let [{status :status sa :result} (ceptr/handle-xhr e)]
    (if (= status "ok")
      (do (_do-logged-in {:session (s/get-session) :creator [sa]})
          (make-ss-channel sa)
          (ssu/send-signal {:to sa :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "identify"
                            :params {:identifiers {:ss-address sa} :attributes {:name *new-user*}}}) refresh-stream-callback)
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
        (do-logged-in result))
      (do (js/alert result)
          (do-logged-out)))))

(defn do-auth [] (.setVisible auth true))
(def auth (goog.ui.Prompt. "Authenticate" "User"
                           (fn [r]
                             (if (not ( nil? r))
                               (do
                                 (ui/loading-start)
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
                                 (def *new-user* r)
                                 (ceptr/command {:cmd "new-user" :params {:user r}}  new-user-callback))))))

(defn check-auth []
  (let [s (s/get-session)]
    (if (or (= s js/undefined) (nil? s)) (do-auth) (do-logged-in {:session s :creator [(s/get-ss-addr)]}))))
