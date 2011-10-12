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
  (js/alert "picked"))


(defn make-groove-field-id [field-name]
  (let [n (if (keyword? field-name) (name field-name) field-name)]
    (str "groove-field-" n)))

;;TODO: convert fields to use goog UI components
(defn make-groove-field
  "create the dom elments for the input field given a groove field spec"
  [field-name field-type]
  (let [field-id (make-groove-field-id field-name)]
    (d/build
     [:p 
      [:label {:for field-id} (str (string/capitalize (name field-name)) ": ")]
      (cond
       (= field-type "text/html") [(keyword (str "textarea#" field-id)) {:name field-id :rows 10 :cols 100}]
       true [(keyword (str "input#" field-id)) {:name field-id :size 100}]
       )])))

(defn render-groove
  "given a groove definition and an element id, renders the groove fields into it"
  [elem-id groove]
  (apply d/replace-children elem-id
         (map (fn [[field-name field-type]] (make-groove-field field-name field-type)) groove))
  )

(defn create
  "creates and renders the droplet create dialog"
  [channel]
  (let [scapes (:scapes s/*current-state*)
        identity-names (:values (:ident-name-scape scapes))
        channel-address-identity-scape (:values (:ss-address-ident-scape scapes))
        channel-scape (:values (:channel-scape scapes))
        scape-names (map (fn [[cn _]] (name cn)) channel-scape)
        channel-select (map (fn [[cn ca]] [:option {:value (name cn)} (name cn)]) channel-scape)
        addr-select (map (fn [[ea ia]] [:option {:value (str ia)} (str ((keyword ia) identity-names) "<"(name ea) ">")]) channel-address-identity-scape)
        [channel-select channel-select-element] (ui/make-select "channel" "Choose a channel:" scape-names xxx)
        [groove-select groove-select-element] (ui/make-select "groove" nil (map #(name %) (keys s/*grooves*))
                                                              (fn [e]
                                                                (render-groove :droplet-content (get-selected-groove))))
        ]
    (def *groove-select* groove-select)
    (ui/modal-dialog "droplet-create"
                     [[:h3 "Create Droplet"]
                      [:p [:label {:for "channel"} "Channel: "]
                       channel-select-element]
                      [:p [:label {:for "groove"} "Groove: "]
                       groove-select-element]
                      [:p [:label {:for "from"} "From: "]
                       (apply conj [:select#from {:name "from"}] addr-select)]
                      [:p [:label {:for "to"} "To: "]
                       (apply conj [:select#to {:name "to"}] addr-select)]
                      [:div#droplet-content ""]
                      [:p (ui/make-button "Add to Stream" (fn [] (send (if (nil? channel) "streamscapes" "streamscapes"))))]
                      ]
                     )
    (.setValue groove-select "subject-body-message")    
    (render-groove :droplet-content (:streamscapes (:subject-body-message s/*grooves*)))
    ))

(defn incorporate-callback [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (if (= status "ok")
      (sss/refresh-stream)
      (js/alert result))))

(defn get-selected-groove []
  (:streamscapes ((keyword (. *groove-select* (getValue))) s/*grooves*)))

(defn get-groove-content
  "gets the values of the groove from the dom and loads them into a map by field name"
  [groove]
  (into {} (map (fn [[k _]] [k (. (d/get-element (keyword (make-groove-field-id k))) value)]) groove))
  )

(defn send [channel]
  (let [to (. (d/get-element :to) value)
        from (. (d/get-element :from) value)
        groove (get-selected-groove)]
    (ssu/send-ss-signal {:aspect "matrice" :signal "incorporate"
                         :params {:deliver :immediate :to (js/parseInt to) :from (js/parseInt from) :channel channel :envelope groove
                                  :content (get-groove-content groove)}}
                        incorporate-callback)
    (ui/cancel-modal)))
