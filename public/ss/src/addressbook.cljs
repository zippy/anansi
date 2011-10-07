
(ns ss.addressbook
  (:require [clojure.browser.dom :as dom]
            [clojure.string :as string]
            [ss.dom-helpers :as tdom]
            [goog.events :as events]
            [goog.style :as style]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.makezip :as z]
            ))

(defn render-channel-addresses [state channel-identity-scape ident-addr]
  (let [scape (:values (channel-identity-scape (:scapes state)))
        addresses (filter (fn [[_ ia]] (= (keyword ia) ident-addr)) scape)]
    (if (empty? addresses)
      nil
      [:div.channel-addresses [:p.channel (first (string/split (name channel-identity-scape) #"-"))]
       (into [:p.addresses] (map (fn [[addr _]] [:span.address (name addr)]) addresses))])))


(defn open [state]
  (let [scapes (:scapes state)
        identity-names (:values (:ident-name-scape scapes))
        channel-identity-scapes (filter (fn [scape] (re-find #"-ident-scape$" (name scape))) (keys scapes))
        ]
    (tdom/insert-at (dom/get-element :everything)
                    (tdom/build [:div#modalmask.overlay-mask
                                 [:div#addressbook
                                  [:div.top-right-controls (tdom/html "<button onclick=\"ss.addressbook.close()\">Close</button>""")]
                                  [:h3 "ADDRESSBOOK"]
                                  (into [:div#names] (map (fn [[ident-addr id-name]] [:div.identity [:h4 id-name]
                                                                         (into [:div.channel-addresses-container]
                                                                               (filter (fn [x] (not (nil? x))) (map (fn [cs] (render-channel-addresses state cs ident-addr)) channel-identity-scapes)))
                                                                         ]) identity-names))
                                  ]]) 0))
  )
(defn close []
  (tdom/remove-node :modalmask))

