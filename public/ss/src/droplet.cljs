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

;;TODO: this should be converted to using goog components...,
;;including a nice EditField for html...
;;TODO: needs to create fields based on grammars that the channel understands
(defn create [channel]
  (let [scapes (:scapes s/*current-state*)
        identity-names (:values (:ident-name-scape scapes))
        channel-address-identity-scape (:values (:ss-address-ident-scape scapes))
        addr-select (map (fn [[ea ia]] [:option {:value (str ia)} (str ((keyword ia) identity-names) "<"(name ea) ">")]) channel-address-identity-scape)
        ]
    (ui/modal-dialog "droplet-create"
                     [[:h3 "Create Droplet"]
                      [:p [:label {:for "to"} "To: "]
                       (apply conj [:select#to {:name "to"}] addr-select)]
                      [:p [:label {:for "from"} "From: "]
                       (apply conj [:select#from {:name "from"}] addr-select)]
                      [:p 
                       [:label {:for "subject"} "Subject:"]
                       [:input#subject {:name "subject" :size 100}]]
                      [:p
                       [:textarea#body {:name "body" :rows 20 :cols 100}]]
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
