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
                        (ui/make-button "New E-mail Channel" #(email-form :compose-work nil make-email))
                        (ui/make-button "New Twitter Channel" #(twitter-form :compose-work nil make-twitter))
                        (ui/make-button "New IRC Channel" #(irc-form :compose-work nil make-irc))
                        ]
                       (apply conj [:div.channels]
                              (map (fn [[cname caddr]]
                                     (let [type (ssu/get-channel-type caddr)
                                           chan ((keyword caddr) (:receptors s/*current-state*))
                                           ]
                                       [:div.channel
                                        (d/html (ssu/channel-icon-html cname type))
                                        (ui/make-click-link (name cname)
                                                            (fn [] (condp = type
                                                                      :irc (irc-form :compose-work
                                                                                     (assoc (get-vals-from-controller chan) :name (name cname))
                                                                                     (fn [p] (update-channel caddr p)))
                                                                      :email (email-form :compose-work
                                                                                         (assoc (get-email-vals chan) :name (name cname))
                                                                                         (fn [p] (update-email caddr p))
                                                                                         )
                                                                      :twitter (twitter-form :compose-work
                                                                                             (assoc (get-vals-from-controller chan) :name (name cname))
                                                                                             (fn [p] (update-channel caddr p)))
                                                                    (js/alert "Updating not implemented for this channel!"))
                                                              )
                                                            )]))
                                   (filter (fn [[n _]] (not= n :streamscapes)) (:values (:channel-scape scapes))))
                              )
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
                        (ui/make-button "New Tag" #(tag-form :tag-work nil))
                        (apply conj [:div.my-tags] (map (fn [tag-name] [:p.tag (ui/make-click-link tag-name #(tag-form :tag-work tag-name))])
                                                        (filter #(not (= % "touched")) (vals (:values (:tag-scapes-scape scapes)))))) 
                        ]
                       [:div#tag-work {:style "display:none"} ""]]
                     
                     )))

(defn get-vals-from-controller [chan]
  (let [a (keyword (first (-> chan :scapes :controller-scape :values :controller)))]
    (-> chan :receptors a)))

(defn get-email-vals [chan]
  (let [oa (keyword (first (-> chan :scapes :deliverer-scape :values :deliverer)))
        ia (keyword (first (-> chan :scapes :receiver-scape :values :receiver)))
        i (-> chan :receptors ia)
        o (-> chan :receptors oa)
        fs ["host" "account" "password" "protocol" "port"]
        ]
    (merge (into {} (map (fn [f] [(keyword (str "in-" f)) ((keyword f) i)]) fs))
           (map (fn [f] [(keyword (str "out-" f)) ((keyword f) o)]) fs)
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

(defn build-tag-scape-name [tag-name]
  (str (string/replace (string/replace (string/lower-case tag-name) #"\W"+ "-") #"[^a-zA-Z0-9_-]" "X") "-tag")
  )

(defn make-tag [{tag-name :tag-name}]
  (let [tag-scape-name (build-tag-scape-name tag-name)]
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

(defn update-tag [orig-name {tag-name :tag-name}]
  (if (not (= orig-name tag-name))
    (let [orig-tag-scape-name (build-tag-scape-name orig-name)
          tag-scape-name (build-tag-scape-name tag-name)]
      (ceptr/start-chain
       {:cleanup  sss/refresh-stream
        :error (fn [result] (js/alert (str "Server reported error:" result)))
        :chain [
                (fn [result chain]
                  (ssu/send-ss-signal {:aspect "setup" :signal "rename-scape"
                                       :params {:name orig-tag-scape-name :new-name tag-scape-name}} (ceptr/nextc chain)))
                (fn [result chain]
                  (ssu/send-ss-signal {:aspect "scape" :signal "delete"
                                       :params {:name :tag-scapes :key orig-tag-scape-name}} (ceptr/nextc chain)))
                (fn [result chain]
                  (ssu/send-ss-signal {:aspect "scape" :signal "set"
                                       :params {:name :tag-scapes :key tag-scape-name :address tag-name}} (ceptr/nextc chain)))
                ]}))))

(defn tag-form [parent-id name]
  (ui/make-dialog parent-id
                  [{:field :tag-name :default name :label "Tag Name"}]
   (if (nil? name) make-tag #(update-tag name %)))
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
        params {:type :twitter
                :name (if (u/blank? (:name p)) (str "twitter-" q) (:name p))
                :search-query q}]
    (ssu/send-ss-signal {:aspect "setup" :signal "new-channel"
                  :params params} sss/refresh-stream-callback)))

(defn twitter-form [parent-id defs fun]
  (let [defaults (if (nil? defs)
                   {:search-query "metacurrency"}
                   defs)]
    (ui/make-dialog parent-id
                    [{:field :name :default (:name defaults) :label "Channel Name:" :hint "<twitter search>"}
                     {:field :search-query :default (:search-query defaults) :label "Twitter search query"}]
                    fun)))

(defn make-irc [params]
  (ssu/send-ss-signal {:aspect "setup" :signal "new-channel"
                      :params (merge {:type :irc} params)} sss/refresh-stream-callback))

(defn update-channel [channel-address params]
  (ssu/send-ss-signal {:aspect "setup" :signal "update-channel"
                       :params (assoc  params :channel-address channel-address)} sss/refresh-stream-callback))

(defn irc-form [parent-id defs fun]
  (let [defaults (if (nil? defs)
                   {:name "irc-freenode" :host "irc.freenode.net" :port 6667 :user "Eric" :nick "erichb"}
                   defs
                   ) ]
    (ui/make-dialog parent-id
                    [{:field :name :default (:name defaults) :label "Channel Name:"}
                     {:field :host :default (:host defaults) :label "IRC host:"}
                     {:field :port :default (:port defaults) :label "Port:"}
                     {:field :user :default (:user defaults) :label "IRC User" :hint "<your user name>"}
                     {:field :nick :default (:nick defaults) :label "IRC Nick" :hint "<your nick>"}
                     ]
                    fun
                    )))

(defn update-email [channel-address p]
  (let [params {:in {:host (:in-host p) :account (:in-account p) :password (:in-password p) :protocol (:in-protocol p) :port (:in-port p)}
                :out {:host (:out-host p) :account (:out-account p) :password (:out-password p) :protocol (:out-protocol p) :port (:out-port p)}}]
    (ssu/send-ss-signal {:aspect "setup" :signal "update-channel"
                         :params (assoc  params :channel-address channel-address :name (:name p))} sss/refresh-stream-callback)))
(defn make-email [p]
  (let [params {:type :email :name (:name p)
                :in {:host (:in-host p) :account (:in-account p) :password (:in-password p) :protocol (:in-protocol p) :port (:in-port p)}
                :out {:host (:out-host p) :account (:out-account p) :password (:out-password p) :protocol (:out-protocol p) :port (:out-port p)}}]
    (ssu/send-ss-signal {:aspect "setup" :signal "new-channel"
                  :params params} sss/refresh-stream-callback)))

(defn email-form [parent-id defs fun]
  (let [defaults (if (nil? defs)
                   {:name "gmail" :in-host "imap.gmail.com" :in-protocol "imaps" :in-port ""
                    :out-host "smtp.googlemail.com" :out-protocol "smtps" :out-port 465}
                   defs
                   ) ]
    (ui/make-dialog parent-id
                    [{:field :name :default (:name defaults) :label "Channel Name:"}
                     {:field :in-host :default (:in-host defaults) :label "Incoming Mail server host:"}
                     {:field :in-account :default (:in-account defaults) :hint "<email address>" :label "Incoming Mail server account:"}
                     {:field :in-password :hint "<your password>" :label "Password:"}
                     {:field :in-protocol :default (:in-protocol defaults) :hint "<pop3 or imaps>" :label "Incoming Mail sever protocol:"}
                     {:field :in-port :default (:in-port defaults) :label "Incoming Mail sever port:"}
                     {:field :out-host :default (:out-host defaults) :label "Outgoing Mail server host:"}
                     {:field :out-account :default (:out-account defaults) :hint "<email>" :label "Outgoing Mail server account:"}
                     {:field :out-password :hint "<password>" :label "Password:"}
                     {:field :out-protocol :default (:out-protocol defaults) :label "Outgoing Mail sever protocol:"}
                     {:field :out-port :default (:in-port defaults) :label "Outgoing Mail sever port:"}
                     ]
                    fun)))
