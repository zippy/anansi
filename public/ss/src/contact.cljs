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

(defn get-addresses-by-channel-contact-scape
  [channel-contact-scape contact-address]
  (map (fn [[addr _]] (name addr))  (filter (fn [[_ ia]] (= (keyword ia) contact-address)) channel-contact-scape)))

(defn get-channel-type-from-channel-contact-scape-name [channel-contact-scape-name]
  (let [[_ m] (re-find #"^(.*)-address-contact-scape$" (name channel-contact-scape-name))]
    (if (nil? m) (throw (RuntimeException. (str "scape doesn't appear to be an address-contact-scape: " channel-contact-scape-name))))
    m))

(defn render-channel-addresses [channel-contact-scape-name contact-addr]
  (let [scape (:values (channel-contact-scape-name (:scapes s/*current-state*)))
        addresses (get-addresses-by-channel-contact-scape scape contact-addr)]
    (if (empty? addresses)
      nil
      [:div.channel-addresses [:p.channel (str (name (get-channel-type-from-channel-contact-scape-name channel-contact-scape-name)) ": ") ]
       (into [:p.addresses] (map (fn [addr] [:span.address addr]) addresses))])))

;;TODO: The channel type problem rears it's ugly head yet again!!!!
(defn get-addresses-from-form []
  (let [address-type-ids (map (fn [ct] (keyword (str (name ct) "-address"))) (ssu/get-channel-types))]
    (into {} (keep identity (for [tid address-type-ids]
                                              (let [val (. (d/get-element tid) value)]
                                                (if (and val (not= val ""))
                                                  (condp = tid
                                                      :streamscapes-address [tid (js/parseInt val)]
                                                      [tid val])
                                                  nil)))))))
(defn do-new-address []
  (let [identifiers (get-addresses-from-form)]
    (ssu/send-ss-signal {:aspect "matrice" :signal "identify"
                         :params {:identifiers identifiers
                                  :attributes {:name (. (d/get-element :name) value)}}} sss/refresh-stream-callback)
    (close-contact-form))
  )

(defn get-contact-name [contact-addr]
  ((keyword contact-addr) (:values (:contact-name-scape (:scapes s/*current-state*))))
  )

(defn do-save-contact [contact-addr]
  (let [old-contact-name (get-contact-name contact-addr)
        new-contact-name (. (d/get-element :name) value)]
    (if (not= old-contact-name new-contact-name)
      (ssu/send-ss-signal {:aspect "scape" :signal "set"
                           :params {:name :contact-name :key (js/parseInt (name contact-addr)) :address new-contact-name}} )
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
  (first (get-addresses-by-channel-contact-scape
          (ssu/get-channel-contact-scape-from-type channel-type)
          contact-address)))

(defn contact-form [ok-fun contact-addr contact-name]
  (d/append (d/get-element :contact-form)
            (d/build [:div
                      (ui/make-input "Name" "name" 80 contact-name)
                      (d/build (into [:div.channels] (map (fn [ct] (let [tn (name ct)] (ui/make-input (str tn " Address") (str tn "-address") 40 (get-contact-address contact-addr ct)))) (ssu/get-channel-types))))
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
        contact-names (:values (:contact-name-scape scapes))
        channel-contact-scapes (ssu/get-matching-scapes #"-address-contact-scape$")
        my-ca (s/get-my-contact-address)
        ]
    (ui/modal-dialog
     "contacts"
     ["CONTACTS" [:div.top-right-controls (ui/make-button "New" new-contact)] [:div#contact-form {:style "display:none"} ""] ]
     [(into [:div#names] (map (fn [[caddr cname]]
                                (into [] (keep identity
                                               [:div.contact

                                                [:div.contact-name
                                                 (ui/make-click-link cname #(contact-form (fn [] (do-save-contact caddr)) caddr cname))
                                                 ]
                                                (let [ca (js/parseInt (name caddr))]
                                                  (if (= ca my-ca) nil 
                                                      [:div.delete-contact
                                                       (ui/make-click-link "delete" #(ui/confirm-dialog (str "Are you sure you want to delete the contact: " cname)
                                                                                                        (fn [e] (if (= (.key e) "ok")
                                                                                                                 (ssu/send-ss-signal {:aspect "matrice" :signal "delete-contact"
                                                                                                                                      :params {:address ca}} )
                                                                                                                 ))))]))
                                                (into [:div.channel-addresses-container]
                                                      (filter (fn [x] (not (nil? x))) (map (fn [cs] (render-channel-addresses cs caddr)) channel-contact-scapes)))
                                                ]))) contact-names))])
    )
  )
