(ns ss.ident
  (:require [clojure.browser.dom :as dom]
            [clojure.string :as string]
            [ss.dom-helpers :as d]
            [goog.events :as events]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.ui :as ui]
            [ss.ss-utils :as ssu]
            [ss.state :as s]
            ))

(defn get-my-ident []
  1
  )

(defn render-channel-addresses [channel-identity-scape ident-addr]
  (let [scape (:values (channel-identity-scape (:scapes s/*current-state*)))
        addresses (filter (fn [[_ ia]] (= (keyword ia) ident-addr)) scape)]
    (if (empty? addresses)
      nil
      [:div.channel-addresses [:p.channel (first (string/split (name channel-identity-scape) #"-"))]
       (into [:p.addresses] (map (fn [[addr _]] [:span.address (name addr)]) addresses))])))

;;TODO: this now assumes you can only create ss addresses.  Need to
;;generalized to all the channel types
(defn do-new-address []
  (do (ssu/send-ss-signal {:aspect "matrice" :signal "identify"
                           :params {:identifiers {:ss-address (js/parseInt (. (d/get-element :ssa) value))}
                                    :attributes {:name (. (d/get-element :name) value)}}})
      (d/remove-children :addr-work))
  )

(defn new-address []
  (d/append (d/get-element :addr-work)
            (d/build [:div
                      (ui/make-input "Name" "name" 80)
                      (ui/make-input "SS Addr" "ssa" 5)
                      (ui/make-button "Cancel" #(d/remove-children :addr-work))
                      (ui/make-button "OK" do-new-address)]))
  (d/show :addr-work)
  )

(defn open []
  (let [scapes (:scapes s/*current-state*)
        identity-names (:values (:ident-name-scape scapes))
        channel-identity-scapes (filter (fn [scape] (re-find #"-ident-scape$" (name scape))) (keys scapes))
        ]
    (ui/modal-dialog
     "contacts"
     [[:div.top-right-controls (ui/make-button "New" new-address)] [:div#addr-work {:style "display:none"} ""]
      [:h3 "CONTACTS"]
       (into [:div#names] (map (fn [[ident-addr id-name]] [:div.identity [:h4 id-name]
                                                          (into [:div.channel-addresses-container]
                                                                (filter (fn [x] (not (nil? x))) (map (fn [cs] (render-channel-addresses cs ident-addr)) channel-identity-scapes)))
                                                          ]) identity-names))])
    )
  )
