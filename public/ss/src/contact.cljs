(ns ss.contact
  (:require [clojure.browser.dom :as dom]
            [clojure.string :as string]
            [ss.dom-helpers :as d]
            [goog.events :as events]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.ui :as ui]
            [ss.ss-utils :as ssu]
            [ss.streamscapes :as sss]
            [ss.state :as s]
            ))

(defn get-addresses-by-channel-identity-scape
  [channel-ident-scape contact-address]
  (map (fn [[addr _]] (name addr))  (filter (fn [[_ ia]] (= (keyword ia) contact-address)) channel-ident-scape))
  )

;;TODO: ARG here it is again! see https://github.com/zippy/anansi/issues/7
(defn get-channel-type-from-channel-identity-scape-name [channel-identity-scape-name]
  (condp = 
      (str (first (string/split (name channel-identity-scape-name) #"-")))
      "ss" :streamscapes
      "email" :email
      "twitter" :twitter
      "irc" :irc
      )
  )

(defn render-channel-addresses [channel-identity-scape-name ident-addr]
  (let [scape (:values (channel-identity-scape-name (:scapes s/*current-state*)))
        addresses (get-addresses-by-channel-identity-scape scape ident-addr)]
    (if (empty? addresses)
      nil
      [:div.channel-addresses [:p.channel (str (name (get-channel-type-from-channel-identity-scape-name channel-identity-scape-name)) ": ") ]
       (into [:p.addresses] (map (fn [addr] [:span.address addr]) addresses))])))

;;TODO: The channel type problem rears it's ugly head yet again!!!!
(defn get-addresses-from-form []
  (let [address-type-ids (map (fn [ct] (keyword (str (name ct) "-addr"))) (ssu/get-channel-types))]
    (into {} (keep identity (for [tid address-type-ids]
                                              (let [val (. (d/get-element tid) value)]
                                                (if (and val (not= val ""))
                                                  (condp = tid
                                                      :streamscapes-addr [:ss-address (js/parseInt val)]
                                                      :email-addr [:email val]
                                                      :twitter-addr [:twitter val]
                                                      :irc-addr [:irc val])
                                                  nil)))))))
(defn do-new-address []
  (let [address-type-ids (map (fn [ct] (keyword (str (name ct) "-addr"))) (ssu/get-channel-types))
        identifiers (get-addresses-from-form)]
    (ssu/send-ss-signal {:aspect "matrice" :signal "identify"
                         :params {:identifiers identifiers
                                  :attributes {:name (. (d/get-element :name) value)}}} sss/refresh-stream-callback)
    (close-contact-form))
  )

(defn get-contact-name [contact-addr]
  ((keyword contact-addr) (:values (:ident-name-scape (:scapes s/*current-state*))))
  )

(defn do-save-contact [contact-addr]
  (let [old-contact-name (get-contact-name contact-addr)
        new-contact-name (. (d/get-element :name) value)]
    (if (not= old-contact-name new-contact-name)
      (ssu/send-ss-signal {:aspect "scape" :signal "set"
                           :params {:name :ident-name :key (js/parseInt (name contact-addr)) :address new-contact-name}} )
      )
    (ssu/send-ss-signal {:aspect "matrice" :signal "scape-contact"
                           :params {:address (js/parseInt (name contact-addr)) :identifiers (get-addresses-from-form)}} )
    (close-contact-form)
    )
  )

(defn new-contact []
  (contact-form do-new-address nil nil))

(defn get-contact-address
  [contact-address channel-type]
  (first (get-addresses-by-channel-identity-scape
          (ssu/get-channel-ident-scape-from-type channel-type)
          contact-address)))

(defn contact-form [ok-fun contact-addr contact-name]
  (d/append (d/get-element :contact-form)
            (d/build [:div
                      (ui/make-input "Name" "name" 80 contact-name)
                      (d/build (into [:div.channels] (map (fn [ct] (let [tn (name ct)] (ui/make-input (str tn " Address") (str tn "-addr") 40 (get-contact-address contact-addr ct)))) (ssu/get-channel-types))))
                      (ui/make-button "Cancel" close-contact-form)
                      (ui/make-button "OK" ok-fun)]))
  (d/show :contact-form)
  )

(defn close-contact-form []
  (d/hide :contact-form)
  (d/remove-children :contact-form)
  )

(defn open []
  (let [scapes (:scapes s/*current-state*)
        contact-names (:values (:ident-name-scape scapes))
        channel-identity-scapes (ssu/get-matching-scapes #"-ident-scape$")
        ]
    (ui/modal-dialog
     "contacts"
     [[:div.top-right-controls (ui/make-button "New" new-contact)] [:div#contact-form {:style "display:none"} ""]
      [:h3 "CONTACTS"]
      (into [:div#names] (map (fn [[caddr cname]] [:div.contact
                                                  [:div.contact-name cname (ui/make-button "Edit" #(contact-form (fn [] (do-save-contact caddr)) caddr cname))]
                                                  (into [:div.channel-addresses-container]
                                                        (filter (fn [x] (not (nil? x))) (map (fn [cs] (render-channel-addresses cs caddr)) channel-identity-scapes)))
                                                  ]) contact-names))])
    )
  )
