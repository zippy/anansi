(ns ss.droplet
  (:require [clojure.string :as string]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.dom-helpers :as d]
            [ss.ceptr :as ceptr]
            [ss.ui :as ui]
            [ss.ss-utils :as ssu]
            [ss.streamscapes :as sss]
            [ss.ident :as ident]
            [ss.state :as s]
            ))

(defn xxx [e]
  (js/alert ""))
;;TODO: this should be converted to using goog components...,
;;including a nice EditField for html...
;;TODO: needs to create fields based on grammars that the channel understands
(defn create [channel]
  (let [scapes (:scapes s/*current-state*)
        identity-names (:values (:ident-name-scape scapes))
        channel-address-identity-scape (:values (:ss-address-ident-scape scapes))
        channel-scape (:values (:channel-scape scapes))
        scape-names (map (fn [[cn _]] (name cn)) channel-scape)
        channel-select (map (fn [[cn ca]] [:option {:value (name cn)} (name cn)]) channel-scape)
        addr-select (map (fn [[ea ia]] [:option {:value (str ia)} (str ((keyword ia) identity-names) "<"(name ea) ">")]) channel-address-identity-scape)
        [channel-select channel-select-element] (ui/make-select "channel" "Choose a channel:" scape-names xxx)
        [grammar-select grammar-select-element] (ui/make-select "grammar" nil ["subject-body-message" "im-message" "status-update"] xxx)
        ]
    (.setValue grammar-select "subject-body-message")
    (ui/modal-dialog "droplet-create"
                     [[:h3 "Create Droplet"]
                      [:p [:label {:for "channel"} "Channel: "]
                       channel-select-element
                       ] ;(apply conj [:select#channel {:name "channel"}] channel-select)
                      [:p [:label {:for "grammar"} "Grammar: "]
                       grammar-select-element]
                      [:p [:label {:for "from"} "From: "]
                       (apply conj [:select#from {:name "from"}] addr-select)]
                      [:p [:label {:for "to"} "To: "]
                       (apply conj [:select#to {:name "to"}] addr-select)]
                      [:div#droplet-content
                       [:p 
                        [:label {:for "subject"} "Subject:"]
                        [:input#subject {:name "subject" :size 100}]]
                       [:p
                        [:textarea#body {:name "body" :rows 10 :cols 100}]]]
                      [:p (ui/make-button "Add to Stream" (fn [] (send (if (nil? channel) "streamscapes" "streamscapes"))))]
                      ]
                     )))

(defn incorporate-callback [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (if (= status "ok")
      (sss/refresh-stream)
      (js/alert result)
      )
    )
  )
(defn send [channel]
  (let [to (. (d/get-element :to) value)
        from (. (d/get-element :from) value)
        subject (. (d/get-element :subject) value)
        body (. (d/get-element :subject) value)]
    (ssu/send-ss-signal {:aspect "matrice" :signal "incorporate"
                         :params {:deliver :immediate :to (js/parseInt to) :from (js/parseInt from) :channel channel :envelope {:subject "text/plain" :body "text/html"}
                                  :content {:subject subject :body body}}}
                        incorporate-callback)
    (ui/cancel-modal)))
