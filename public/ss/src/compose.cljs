(ns ss.compose
  (:require [clojure.string :as string]
            [ss.dom-helpers :as d]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.ui :as ui]
            [ss.ss-utils :as ssu]
            [ss.streamscapes :as sss]
            [ss.ceptr :as ceptr]
            [ss.state :as s]
            ))

(defn open
  "displays the compose dialog"
  []
  (let [scapes (:scapes s/*current-state*)]
    (ui/modal-dialog "compose" "SETUP"
                     [
                       [:div [:h4 "Channels"]
                        (ui/make-button "New E-mail Channel" #(make-email-channel :compose-work))
                        (ui/make-button "New Twitter Channel" #(make-twitter-channel :compose-work))
                        (ui/make-button "New IRC Channel" #(make-irc-channel :compose-work))
                        ]
                       [:div#compose-work {:style "display:none"} ""]
                       [:div [:h4 "Scapes"]
                        (ui/make-button "New Scape" #(make-scape :scape-work))
                        (into [:div.my-scapes] 
                              (map (fn [sn] (let [{key-rel :key value-rel :address} (:relationship ((keyword (str (name sn) "-scape")) scapes))]
                                             (debug/alert (sn scapes))
                                             [:p (str (name sn) ": " key-rel "->" value-rel " ")
                                              (ui/make-button "Add Scape Entry" #(set-scape sn key-rel value-rel :scape-work))
                                              ])) (keys (:values (:my-scapes-scape scapes)))))]
                       [:div#scape-work {:style "display:none"} ""]
                       [:div [:h4 "Tags"]
                        (ui/make-button "New Tag" #(make-tag :tag-work))
                        ]
                       [:div#tag-work {:style "display:none"} ""]]
                     
                     )))

(defn do-make-scape [{scape-name :scape-name key-rel :key-rel val-rel :value-rel}]
  (ceptr/start-chain
   {:cleanup  sss/refresh-stream
    :error (fn [result] (js/alert (str "Server reported error:" result)))
    :chain [ 
            (fn [result chain]
              (ssu/send-ss-signal {:aspect "setup" :signal "new-scape"
                                   :params {:name scape-name :relationship {:key key-rel :address val-rel}}} (ceptr/nextc chain)))
             (fn [result chain]
               (ssu/send-ss-signal {:aspect "scape" :signal "set"
                                    :params {:name "my-scapes" :key scape-name :address true}} (ceptr/nextc chain)))]}))

(defn make-scape [parent-id]
  (ui/make-dialog parent-id
                  [{:field :scape-name :label "Scape Name" :hint "name should be singular"}
                   {:field :key-rel :label "Key Relationship" :hint "dash-separated-type"}
                   {:field :value-rel :label "Value Relationship" :hint "dash-separated-type"}]
   do-make-scape)
  )

(defn do-make-tag [{tag-name :tag-name}]
  (let [tag-scape-name (str (string/replace (string/replace (string/lower-case tag-name) #"\W"+ "-") #"[^a-zA-Z0-9_-]" "X") "-tag")]
    (ceptr/start-chain
     {:cleanup  sss/refresh-stream
      :error (fn [result] (js/alert (str "Server reported error:" result)))
      :chain [
              (fn [result chain]
                (ssu/send-ss-signal {:aspect "setup" :signal "new-scape"
                                     :params {:name tag-scape-name :relationship {:key "droplet-address" :address "boolean"}}} (ceptr/nextc chain)))
              (fn [result chain]
                (ssu/send-ss-signal {:aspect "scape" :signal "set"
                                     :params {:name :tag-scapes :key tag-scape-name :address tag-name}} (ceptr/nextc chain)))
              ]})))

(defn make-tag [parent-id]
  (ui/make-dialog parent-id
                  [{:field :tag-name :label "Tag Name"}]
   do-make-tag)
  )

(defn do-set-scape [scape-name {key :key-val val :value-val}]
  (ssu/send-ss-signal {:aspect "scape" :signal "set"
                       :params {:name scape-name :key key :address val}} sss/refresh-stream-callback))

(defn set-scape [scape-name key-rel val-rel parent-id]
  (ui/make-dialog parent-id
                  [{:field :key-val :label "Key" :hint key-rel}
                   {:field :value-val :label "Value" :hint val-rel}]
   (fn [p] (do-set-scape scape-name p))))

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
