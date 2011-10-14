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

(defn render-channel-addresses [channel-identity-scape-name ident-addr]
  (let [scape (:values (channel-identity-scape-name (:scapes s/*current-state*)))
        addresses (get-addresses-by-channel-identity-scape scape ident-addr)]
    (if (empty? addresses)
      nil
      [:div.channel-addresses [:p.channel (first (string/split (name channel-identity-scape-name) #"-"))]
       (into [:p.addresses] (map (fn [addr] [:span.address addr]) addresses))])))

;;TODO: The channel type problem rears it's ugly head yet again!!!!
(defn do-new-address []
  (let [address-type-ids (map (fn [ct] (keyword (str (name ct) "-addr"))) (sss/get-channel-types))
        identifiers (into {} (keep identity (for [tid address-type-ids]
                                              (let [val (. (d/get-element tid) value)]
                                                (if (and val (not= val ""))
                                                  (condp = tid
                                                      :streamscapes-addr [:ss-address (js/parseInt val)]
                                                      :email-addr [:email val]
                                                      :twitter-addr [:twitter val]
                                                      :irc-addr [:irc val])
                                                  nil)))))]
    (ssu/send-ss-signal {:aspect "matrice" :signal "identify"
                         :params {:identifiers identifiers
                                  :attributes {:name (. (d/get-element :name) value)}}})
    (close-contact-form))
  )

(defn do-save-contact [contact-id]
  )

(defn new-contact []
  (contact-form do-new-address nil nil))

(defn get-contact-address
  [contact-address channel-type]
  (first (get-addresses-by-channel-identity-scape
          (sss/get-channel-ident-scape-from-type channel-type)
          contact-address)))

(defn contact-form [ok-fun contact-id contact-name]
  (d/append (d/get-element :contact-form)
            (d/build [:div
                      (ui/make-input "Name" "name" 80 contact-name)
                      (d/build (into [:div.channels] (map (fn [ct] (let [tn (name ct)] (ui/make-input (str tn " Address") (str tn "-addr") 40 (get-contact-address contact-id ct)))) (sss/get-channel-types))))
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
        channel-identity-scapes (filter (fn [scape] (re-find #"-ident-scape$" (name scape))) (keys scapes))
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
