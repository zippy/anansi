(ns ss.compose
  (:require [clojure.string :as string]
            [ss.dom-helpers :as d]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.ui :as ui]
            [ss.ss-utils :as ssu]
            [ss.streamscapes :as sss]
            ))

(defn open
  "displays the compose dialog"
  []
  (ui/modal-dialog "compose"
                   [ [:h3 "COMPOSE"]
                     [:div [:h4 "Create Channels"]
                      (ui/make-button "New E-mail Channel" #(make-email-channel :compose-work))
                      (ui/make-button "New Twitter Channel" #(make-twitter-channel :compose-work))
                      (ui/make-button "New IRC Channel" #(make-irc-channel :compose-work))
                      ]
                     [:div#compose-work {:style "display:none"} ""]]
                   ))

(defn make-twitter [p]
  (let [q (:search-query p)
        params {:type :twitter :name (str "twitter-" q) :search-query q}]
    (ssu/send-ss-signal {:aspect "setup" :signal "new-channel"
                  :params params} sss/refresh-stream-callback)))

(defn make-twitter-channel [parent-id]
  (ui/make-dialog parent-id
   [{:field :search-query :default "#metacurrency" :label "Twitter search query"}]
   make-twitter))

(defn make-irc [params]
  (ssu/send-ss-signal {:aspect "setup" :signal "new-channel"
                      :params (merge {:type :irc} params)} sss/refresh-stream-callback))

(defn make-irc-channel [parent-id]
  (ui/make-dialog parent-id
                  [{:field :name :default "irc-freenode" :label "Channel Name:"}
                   {:field :host :default "irc.freenode.net" :label "IRC host:"}
                   {:field :port :default 6667 :label "Port:"}
                   {:field :user :default "Eric" :label "IRC User"}
                   {:field :nick :default "erichb" :label "IRC Nick"}
                   ]
             make-irc))

(defn make-email [p]
  (let [params {:type :email :name (:channel-name p)
                :in {:host (:in-host p) :account (:in-account p) :password (:in-password p) :protocol (:in-protocol p)}
                :out {:host (:out-host p) :account (:out-account p) :password (:out-password p) :protocol (:out-protocol p) :port (:out-port p)}}]
    (ssu/send-ss-signal {:aspect "setup" :signal "new-channel"
                  :params params} sss/refresh-stream-callback)))

(defn make-email-channel [parent-id]
  (ui/make-dialog parent-id
                  [{:field :channel-name :default "email" :label "Channel Name:"}
                   {:field :in-host :default "mail.harris-braun.com" :label "Incoming Mail server host:"}
                   {:field :in-account :default "eric@harris-braun.com" :label "Incoming Mail server account:"}
                   {:field :in-password :label "Incoming Mail sever password:"}
                   {:field :in-protocol :default "pop3" :label "Incoming Mail sever protocol:"}
                   {:field :out-host :default "mail.harris-braun.com" :label "Outgoing Mail server host:"}
                   {:field :out-account :default "eric@harris-braun.com" :label "Outgoing Mail server account:"}
                   {:field :out-password :label "Outogoing Mail sever password:"}
                   {:field :out-protocol :default "smtps" :label "Outgoing Mail sever protocol:"}
                   {:field :out-port :default 25 :label "Outgoing Mail sever port:"}
                   ]
                  make-email))
