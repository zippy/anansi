(ns ss.compose
  (:require [clojure.string :as string]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.dom-helpers :as d]
            [ss.ceptr :as ceptr]
            [ss.ui :as ui]
            [ss.ss-utils :as ssu]
            [ss.streamscapes :as sss]
            [ss.auth :as auth]
            [ss.ident :as ident]
            ))

;;TODO: this should be converted to using goog components...,
;;including a nice EditField for html...
(defn compose []
  (let [scapes (:scapes ssu/*current-state*)
        identity-names (:values (:ident-name-scape scapes))
        channel-address-identity-scape (:values (:ss-address-ident-scape scapes))
        addr-select (map (fn [[ea ia]] [:option {:value (str ia)} (str ((keyword ia) identity-names) "<"(name ea) ">")]) channel-address-identity-scape)
        ]
    (ui/modal-dialog "compose"
                     [[:h3 "Compose Droplet"]
                      [:p [:label {:for "to"} "To: "]
                       (apply conj [:select#to {:name "to"}] addr-select)]
                      [:p [:label {:for "from"} "From: "]
                       (apply conj [:select#from {:name "from"}] addr-select)]
                      [:p 
                       [:label {:for "subject"} "Subject:"]
                       [:input#subject {:name "subject" :size 100}]]
                      [:p
                       [:textarea#body {:name "body" :rows 20 :cols 100}]]
                      [:p (d/html (str "<button onclick=\"ss.compose.send('" (str "streamscapes") "')\">Send</button>"))]
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
    (ssu/send-signal {:to auth/ss-addr :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "incorporate"
                      :params {:deliver :immediate :to (js/parseInt to) :from (js/parseInt from) :channel channel :envelope {:subject "text/plain" :body "text/html"}
                               :content {:subject subject :body body}}}
                     incorporate-callback)
    (ui/cancel-modal)))
