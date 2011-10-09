(ns ss.core
  (:require [clojure.browser.dom :as dom]
            [cljs.reader :as reader]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.addressbook :as ab]
            [ss.ceptr :as ceptr]
            [ss.session :as s]
            [ss.dom-helpers :as d]
            [ss.streamscapes :as sss]
            [ss.ss-utils :as ssu]
            [ss.auth :as auth]
            [ss.ui :as ui]
            [ss.email :as email]
            ))

(defn make-irc [params]
  (ssu/send-signal {:to auth/ss-addr :prefix "streamscapes.streamscapes" :aspect "setup" :signal "new-channel"
                :params (merge {:type :irc, :name :freenode} params)})
  )
(defn make-email [p]
  (let [params {:type :email :name (:channel-name p)
                :in {:host (:in-host p) :account (:in-account p) :password (:in-password p) :protocol (:in-protocol p)}
                :out {:host (:out-host p) :account (:out-account p) :password (:out-password p) :protocol (:out-protocol p) :port (:out-port p)}}]
    (ssu/send-signal {:to auth/ss-addr :prefix "streamscapes.streamscapes" :aspect "setup" :signal "new-channel"
                  :params params}))
  )

(defn make-email-channel []
  (ui/make-dialog
   {:channel-name "email" :in-host "mail.harris-braun.com" :in-account "eric@harris-braun.com" :in-password "pass" :in-protocol "pop3"
    :out-host "mail.harris-braun.com" :out-account "eric@harris-braun.com" :out-password "pass" :out-protocol "smtps" :out-port 25}
   make-email
   ))

(defn refresh-stream-callback [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (sss/refresh-stream)
    )
  )
(defn twitter-check [c]
  (ssu/send-signal {:to auth/ss-addr :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "control-channel"
                :params {:name c :command :check} }
               refresh-stream-callback)
  )

(defn make-twitter [p]
  (let [screen-name (:twitter-name p)
        params {:type :twitter :name (str "twitter-" screen-name) :screen-name screen-name}]
    (ssu/send-signal {:to auth/ss-addr :prefix "streamscapes.streamscapes" :aspect "setup" :signal "new-channel"
                  :params params}))
  )

(defn make-twitter-channel []
  (ui/make-dialog
   {:twitter-name "zippy314"}
   make-twitter      
   )
  )

(defn email-check []
  (ssu/send-signal {:to auth/ss-addr :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "control-channel"
                :params {:name :email :command :check} }
               refresh-stream-callback))

(defn make-irc-channel []
  (ui/make-dialog {:host "irc.freenode.net", :port 6667, :user "Eric", :nick "zippy31415"}
             make-irc       
             ))
(defn make-ss []
  (ui/make-dialog {:name ""}
               (fn [params]
                 (ssu/send-signal {:to 0 :prefix "receptor.host" :aspect "self" :signal "host-streamscape" :params (merge {:matrice-address 7} params)})
                 )))
(defn irc-join []
  (ui/make-dialog {:channel "#ceptr"}
               (fn [params]
                 (ssu/send-signal {:to auth/ss-addr :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "control-channel"
                               :params {:name :freenode :command :join :params params} }))))
(defn irc-open []
  (ssu/send-signal {:to auth/ss-addr :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "control-channel"
                :params {:name :freenode :command :open}}))
(defn irc-close []
  (ssu/send-signal {:to auth/ss-addr :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "control-channel"
                  :params {:name :freenode :command :close}}))






