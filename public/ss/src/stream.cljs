(ns ss.stream
  (:require [clojure.browser.dom :as dom]
            [clojure.string :as string]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.state :as s]
            [ss.dom-helpers :as d]
            [ss.ceptr :as ceptr]
            [ss.ui :as ui]
            [ss.droplet :as droplet]
            [ss.ss-utils :as ssu]
            ))

;; These are the functions that render the streamscapes ui
(defn humanize-ss-datetime [dt]
  (let [[date ltime] (string/split (name dt) #"T")
        [year month day] (string/split date #"-")
        [time _] (string/split ltime #"\.")
        [hour min sec] (string/split time #":")
        ]
    (str month "/" day "/" year " " hour ":" min )))

;;TODO: iniffecient, this scans the receipt scape every time, we should have an inverse lookup...
(defn droplet-date [s d scape]
  (let [r (:values (scape (:scapes s)))
        a (:address d)
        [[t _]] (remove (fn [[_ address]] (not (= address a))) r)
        ]
    (humanize-ss-datetime t)))

(defn resolve-ident [s ident]
  ((keyword ident) (:values (:ident-name-scape (:scapes s)))))

(defn resolve-twitter-avatar [s ident]
  (str "<img class=\"twitter-avatar\" src=\"" ((keyword ident) (:values (:ident-twitter-avatar-scape (:scapes s)))) "\">")   )

(defn get-html-from-body [body content-type]
  (if (re-find #"^multipart" content-type)
    (:content (first (filter (fn [part] (re-find #"^text/html" (:content-type part))) body)))
    (if (re-find #"^text/html" content-type)
      body
      (str "<pre>" body "</pre>"))))

(defn visualize [data]
  (let [
        idv (map vector (iterate inc 0) data)
        table (doto (google.visualization.DataTable.)
                (.addColumn "string" "Date")
                (.addColumn "number" "Flows")
                (.addRows (count data)))
         chart (google.visualization.AreaChart. (ss.dom-helpers/get-element :visualization ))
       ]
     (doseq [[idx [k v]] idv]
      (.setCell table idx 0 k)
      (.setCell table idx 1 v))
    (.draw chart table nil)))


(defn render [refresh-fun]
  (let [s s/*current-state*
        elem (d/get-element :stream-panel )
        scapes (:scapes s)
        droplet-channel-scape (:values (:droplet-channel-scape scapes))
        ]
    (d/remove-children :stream-panel )
    (d/append elem
      (d/build [:div#stream-control [:div#buttons (ui/make-button "Create Droplet" droplet/create)
                                     (ui/make-button "Refresh" refresh-fun)
                                     ]])
      (d/build [:div#flow-panel [:div#visualization {:style "width:550px; height:150px"} ""]
                [:div.count (str "stream: " (count (:receptor-order s))
                  " of " (:receptor-total s))]])
      (d/build [:div#droplet-panel (apply conj [:div.droplet-previews ]
        (map (fn [da] (render-preview (keyword da) droplet-channel-scape s))
          (:receptor-order s)))]))
    (visualize (:frequencies s))
    ))

(defn get-sbmg-body [d]
  (let [body (:body (:content d))
        content-type (:body (:envelope d))
        ]
    (d/build [:div (d/html (get-html-from-body body content-type))])))

(defn render-full
  "renders the full droplet by pulling out the parts of the droplet that are specified by the groove grammar"
  [droplet-address channel-type s]
  (let [d ((:receptors s) droplet-address)
        [groove grammar actions] (get-droplet-grammar d channel-type s)
        parts (map (fn [[part _]] [:div.part [:h4 (name part)]
                                  (d/html (get-html-from-body (part (:content d)) (part (:envelope d))))]) grammar)]
    (ui/modal-dialog "full-droplet"
                     [(str (name channel-type) " droplet") [:div.top-right-controls (ssu/make-tagging-button droplet-address)]] parts)))

(defn render-preview [droplet-address droplet-channel-scape s]
    (let [d ((:receptors s) droplet-address)
          channel-address (droplet-channel-scape droplet-address)
          channel-name (ssu/get-channel-name-from-address channel-address)
          channel-type (ssu/get-channel-type channel-address)
          channel-icon (ssu/channel-icon-html channel-name channel-type)
          sent (droplet-date s d :delivery-scape)
          from (resolve-ident s (:from d))
          tag-name-map (into {} (map (fn [[sn tn]] [tn sn]) (:values (:tag-scapes-scape (:scapes s/*current-state*)))))
          tag-menu-elem (ssu/make-tagging-button droplet-address)
          tags (map #(name %) (ssu/get-droplet-tags droplet-address))
          preview-tag (if (empty? tags) :div.droplet-preview (keyword (str "div.droplet-preview_" (string/join "_" tags))))
          [groove-specific a groove] (groove-preview d channel-type s)
          actions (if (map? a) (map #(name %) (keys a)) a)
          preview [preview-tag
                   [:div.preview-channel-icon (d/html channel-icon)]
                   [:div.preview-from from]
                   (ui/add-click-fun (d/build [:div.preview-groove-specific groove-specific])
                                     #(do(ssu/tag-droplet droplet-address :touched-tag)
                                         (render-full droplet-address channel-type s)))
                   [:div.preview-sent sent]
                   [:div.preview-tags tag-menu-elem]
                   ]
          ]
      (if ((set actions) "reply") (conj preview [:div.preview-actions
                                                 (ui/make-click-link "Reply" #(droplet/create channel-name groove))])  
          preview)
      ))



(defn get-droplet-grammar [d channel-type s]
  (let [dg (:values (:droplet-grooves-scape (:scapes s)))
        groove-name (keyword (first ((keyword (str (:address d))) dg )))
        
        ]
    [groove-name
     (channel-type (groove-name s/*grooves*))
     (channel-type (groove-name s/*groove-actions*))]))

;;TODO: groove droplets should be auto-detected by some appropriate
;;programmatic method, not by channel-type!
(defn groove-preview [d channel-type s]
  (let [[groove grammar actions] (get-droplet-grammar d channel-type s)
        p (if (contains? grammar :subject )
            [:div.subject
             (str (:subject (:content d)))]
            [:div.content (str (if (nil? (:text (:content d)))
                                 (:message (:content d))
                                 (if (nil? (:text (:content d)))
                                   (:description (:content d))
                                   (:text (:content d)))))])
        ]
    [p actions groove]
    ))

;; Actions

(defn categorize [droplet-address scape-name]
  (ssu/send-ss-signal {:aspect "scape" :signal "set"
                       :params {:name (descapify scape-name) :key droplet-address :address true}} refresh-stream-callback))
