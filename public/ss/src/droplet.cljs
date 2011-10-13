(ns ss.droplet
  (:require [clojure.string :as string]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.dom-helpers :as d]
            [ss.ceptr :as ceptr]
            [ss.ui :as ui]
            [ss.ss-utils :as ssu]
            [ss.streamscapes :as sss]
            [ss.contact :as contact]
            [ss.state :as s]
            ))

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
  (let [identity-names (:values (:ident-name-scape (:scapes s/*current-state*)))
        senders (set (map #(js/parseInt (name %)) (keys (:values (:sender-scape (:scapes s/*current-state*))))))
        channel-name (. *channel-select* (getValue))
        channel-address-identity-scape (sss/get-channel-ident-scape channel-name)
;;        addr-select (map (fn [[ea ia]] [:option {:value (str ia)} (str ((keyword ia) identity-names) "<"(name ea) ">")]) channel-address-identity-scape)
        to-addresses (map (fn [[a-name contact-id]] [(str ((keyword contact-id) identity-names) "<"(name a-name) ">") contact-id]) channel-address-identity-scape)
        from-addresses (map (fn [[a-name contact-id]] [(str ((keyword contact-id) identity-names) "<"(name a-name) ">") contact-id])
                          (filter (fn [[_ contact-id]] (senders contact-id)) channel-address-identity-scape))
        [to-select to-select-element] (ui/make-select "to" nil to-addresses (fn [] nil))
        [from-select from-select-element] (ui/make-select "from" nil from-addresses (fn [] nil))
        ]
    (def *to-select* to-select)
    (def *from-select* from-select)
    (apply d/replace-children elem-id
           (d/build [:p [:label {:for "from"} "From: " from-select-element]])
           (d/build [:p [:label {:for "to"} "To: " to-select-element]])
           (map (fn [[field-name field-type]] (make-groove-field field-name field-type)) groove)))
  )


(defn setup-groove
  "setup all aspects of the form based on the currently selected groove"
  []
  (let [groove-name (. *groove-select* (getValue))
        groove-id (keyword groove-name)
        groove (groove-id s/*grooves*)
        selected-channel (. *channel-select* (getSelectedItem))
        groove-spec ((sss/get-channel-type (. selected-channel (getValue))) groove)]
    (doseq [idx (range (. *channel-select* (getItemCount)))]
      (let [item (. *channel-select* (getItemAt idx))
            chan (. item (getValue))
            valid (contains? groove (sss/get-channel-type chan))]
        (. item (setEnabled valid))))
    (let [valid-channel (and selected-channel (. selected-channel (isEnabled)))]
      (if valid-channel
        (render-groove :droplet-content groove-spec)
        (d/remove-children :droplet-content))
      (. *incorp-button* (setEnabled valid-channel)))
    )
  )

(defn create
  "creates and renders the droplet create dialog"
  [channel]
  (let [scapes (:scapes s/*current-state*)
        identity-names (:values (:ident-name-scape scapes))
        channel-address-identity-scape (:values (:ss-address-ident-scape scapes))
        channel-names (sss/get-channel-names)
        [channel-select channel-select-element] (ui/make-select "channel" "Choose a channel:" channel-names setup-groove)
        [groove-select groove-select-element] (ui/make-select "groove" nil (map #(name %) (keys s/*grooves*))
                                                              setup-groove)
        [incorp-button incorp-button-element] (ui/make-button "Add to Stream" send :both)
        ]
    (def *groove-select* groove-select)
    (def *channel-select* channel-select)
    (def *incorp-button* incorp-button)
    (ui/modal-dialog "droplet-create"
                     [[:h3 "Create Droplet"]
                      [:p [:label {:for "groove"} "Groove: "]
                       groove-select-element]
                      [:p [:label {:for "channel"} "Channel: "]
                       channel-select-element]
                      [:div#droplet-content ""]
                      [:p incorp-button-element]
                      ]
                     )
    (.setValue groove-select "subject-body-message")
    (setup-groove)
    ))

(defn incorporate-callback [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (if (= status "ok")
      (sss/refresh-stream)
      (js/alert result))))

(defn get-selected-groove []
  (keyword (. *groove-select* (getValue))))

(defn get-groove-content
  "gets the values of the groove from the dom and loads them into a map by field name"
  [groove]
  (into {} (map (fn [[k _]] [k (. (d/get-element (keyword (make-groove-field-id k))) value)]) groove))
  )

(defn send []
  (let [groove-name (. *groove-select* (getValue))
        groove-id (keyword groove-name)
        groove (groove-id s/*grooves*)
        selected-channel (. *channel-select* (getSelectedItem))
        channel-name (. selected-channel (getValue))
        groove-spec ((sss/get-channel-type channel-name) groove)
        to (. *to-select* (getValue))
        from (. *from-select* (getValue))
        groove (get-selected-groove)]
    (ssu/send-ss-signal {:aspect "matrice" :signal "incorporate"
                         :params {:deliver :immediate :to (js/parseInt to) :from (js/parseInt from) :channel channel-name :envelope groove-spec
                                  :content (get-groove-content groove-spec)}}
                        incorporate-callback)
    (ui/cancel-modal)))
