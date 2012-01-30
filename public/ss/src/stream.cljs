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

(defn resolve-contact [s contact]
  (let [c ((keyword contact) (:values (:contact-name-scape (:scapes s))))]
    (if (nil? c) "[Unknown]" c)))

(defn resolve-twitter-avatar [s contact]
  (str "<img class=\"twitter-avatar\" src=\"" ((keyword contact) (:values (:contact-twitter-avatar-scape (:scapes s)))) "\">")   )

(defn get-html-from-body [body content-type]
  (if (re-find #"^multipart" content-type)
    (:content (first (filter (fn [part] (re-find #"^text/html" (:content-type part))) body)))
    (if (re-find #"^text/html" content-type)
      body
      (str "<pre>" body "</pre>"))))

(defn visualize [data]
  (viz google.visualization.AreaChart {
    :options {:title "Flow rate"} 
    :labels [["Date" "datetime"] ["Flows" "number"]]
    :data data
    }))

(defn viz [viz-type {options :options labels :labels data :data}]
  (let [cols (count labels)
        idv (map vector (iterate inc 0) data)
        table (google.visualization.DataTable.)
        chart (viz-type. (ss.dom-helpers/get-element :visualization ))
        [first-label first-type] (first labels)
        d (if (or (= first-type "date") (= first-type "datetime")) 
          (into [] (map (fn [[r & x]] (into [] (cons (js/Date. r) x))) data))
          data)
       ]
       (doseq [[l t] labels] (.addColumn table t l))
       (.addRows table (ss.utils.clj->js d))
       (.draw chart table (ss.utils.clj->js options))))

(defn page-count [total]
  (+ (quot total s/*items-per-page*) (if (> (mod total s/*items-per-page*) 0) 1 0)))

(defn page-fun [fun]
  (do 
    (s/set-page (fun s/*page* 1))
    (ss.streamscapes/refresh-current-stream)))

(defn build-pager
  "create the dom elments and html for droplet pager based on current items-per-page prefs"
  [s]
  (let [total (:receptor-total s)
        items-per-page s/*items-per-page*
        pager-items (if (< total items-per-page)
                     [(str "stream: " total " droplets")]
                     (let [offset (+ 1 (s/get-offset))
                           pc (page-count total)]
                       [(str "stream: " offset "-"
                             (+ (if (> s/*page* (quot total s/*items-per-page*)) (- (mod total s/*items-per-page*) 1) s/*items-per-page*) offset)
                             " of " total
                             " (page " s/*page* " of " pc ")")
                        (if (> s/*page* 1)
                          [:span.left-arrow (ui/make-button "<"  #(page-fun -))]
                          (d/html ""))
                        (if (< s/*page* pc)
                          [:span.right-arrow (ui/make-button ">" #(page-fun +) )]
                          (d/html "")
                          )

                        ])
                     )]
    (apply conj [:div.pager] pager-items)))

(defn render
  "render the stream panel"
  [refresh-fun]
  (let [s s/*current-state*
        elem (d/get-element :stream-panel )
        scapes (:scapes s)
        droplet-channel-scape (:values (:droplet-channel-scape scapes))
        ]
    (d/remove-children :stream-panel )
    (d/append elem
      (d/build [:div#stream-control [:div#buttons (ui/make-button "Create Droplet" #(droplet/create nil nil))
                                     (ui/make-button "Refresh" refresh-fun)
                                     ]])
      (d/build [:div#flow-panel [:div#visualization {:style "width:550px; height:150px"} ""]
                (build-pager s)])
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
                     [(str (name channel-type) " droplet (" (if (nil? groove) "-no-groove-" (name groove)) ")")
                      [:div.top-right-controls (ssu/make-tagging-button droplet-address)
                       (ui/make-button "Delete" (fn [] (delete-droplet droplet-address #(ui/cancel-modal))))
                       ]] parts))) ;;

(defn render-preview [droplet-address droplet-channel-scape s]
    (let [d ((:receptors s) droplet-address)
          channel-address (droplet-channel-scape droplet-address)
          channel-name (ssu/get-channel-name-from-address channel-address)
          channel-type (ssu/get-channel-type channel-address)
          channel-icon (ssu/channel-icon-html channel-name channel-type)
          sent (droplet-date s d :delivery-scape)
          from (resolve-contact s (:from d))
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
                   [:div.preview-delete (ui/make-click-link "x" #(delete-droplet droplet-address (fn [] nil))) ]
                   [:div.preview-sent sent]
                   [:div.preview-tags tag-menu-elem]
                   ]
          ]
      (if ((set actions) "reply") (conj preview [:div.preview-actions
                                                 (ui/make-click-link "Reply" #(droplet/create channel-name groove))])  
          preview)
      ))



(defn get-droplet-grammar [d channel-type s]
  (let [dgs (:values (:droplet-grooves-scape (:scapes s)))
        dg ((keyword (str (:address d))) dgs )
        groove-name (if (empty? dg) nil (keyword (first dg)))
        ]
    (if (nil? groove-name)
      [nil (:envelope d) {}]
      [groove-name
       (s/get-groove-grammar groove-name)
       (s/get-groove-channel-actions groove-name channel-type)])))

;;TODO: groove droplets should be auto-detected by some appropriate
;;programmatic method, not by channel-type!
(defn groove-preview [d channel-type s]
  (let [[groove grammar actions] (get-droplet-grammar d channel-type s)
        preview (s/get-groove-preview groove)
        c (:content d)
        p (cond (vector? preview)
                [:div.content (apply str (map (fn [i] (if (vector? i) ((keyword (i 0)) (-> d :matched-grooves groove)) i)) preview))]
                (string? preview)
                [:div.content (let [k (keyword preview)
                                    val (k c)]
                                (if (nil? val)
                                  (let [v (k (-> d :matched-grooves groove))]
                                    (if (nil? v) "-blank-message-" v))
                                  val)
                                ) ]
                true
                [:div.content (if (not (nil? (:subject c))) (:subject c)
                                  (if (not (nil? (:body c))) (:body c)
                                      (string/join "; " (map #(name %) (keys (:content d))))))] ;
                )]
    [p actions groove]))

;; Actions

(defn categorize [droplet-address scape-name]
  (ssu/send-ss-signal {:aspect "scape" :signal "set"
                       :params {:name (descapify scape-name) :key droplet-address :address true}} refresh-stream-callback))

(defn delete-droplet [droplet-address ok-fun]
  (ui/confirm-dialog "Are you sure you want to delete this droplet?"
                     (fn [e] (if (= (.key e) "ok")
                              (do (ssu/send-ss-signal {:aspect "matrice" :signal "discorporate"
                                                       :params {:droplet-address  (js/parseInt (name droplet-address))}} ss.streamscapes.refresh-stream-callback)
                                  (ok-fun))
                              ))))

