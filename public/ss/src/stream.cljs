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

(defn zip-for-email-droplet [s d-addr channel]
  (let [d ((:receptors s) d-addr)
        body (:body (:content d))
        content-type (:body (:envelope d))
        html (d/build [:div (d/html (get-html-from-body body content-type))])
        ]
    (ui/make-zips [{:title "Raw" :content (u/clj->json body)}] html)
    {:title (str (ssu/channel-icon-html channel :email) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)) " Subject: " (:subject (:content d)))
     :content (d/build [:div [:div#html html]])}))

(defn zip-for-streamscapes-droplet [s d-addr channel]
  (let [d ((:receptors s) d-addr)
        body (:body (:content d))
        subject (:subject (:content d))
        message (:message (:content d))
        channel-type :streamscapes
        ]
    {:title (str (ssu/channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d))
                 (if (nil? subject) (str " : " message) (str " Subject: " subject)))
     :content (if (nil? body) "No Body" body)}
))

(defn render [refresh-fun]
  (let [s s/*current-state*
        elem (d/get-element :stream-panel)
        scapes (:scapes s)
        droplet-channel-scape (:values (:droplet-channel-scape scapes))
        ]
    (d/remove-children :stream-panel)
    (d/append elem
              (d/build [:div.stream-control
                        (ui/make-button "Create Droplet" droplet/create)
                        (ui/make-button "Refresh" refresh-fun)
                        ])
              (d/build [:h3 (str "stream: " (count droplet-channel-scape) " of " (:receptor-total s))])
              (d/build (apply conj [:div.droplet-previews] (map (fn [da] (render-preview (keyword da) droplet-channel-scape s)) (:receptor-order s)))))

        ))

(defn get-sbmg-body [d]
  (let [body (:body (:content d))
        content-type (:body (:envelope d))
        ]
    (d/build [:div (d/html (get-html-from-body body content-type))])))

(defn render-full
  "renders the full droplet by pulling out the parts of the droplet that are specified by the groove grammar"
  [d channel-type s]
  (let [grammar (get-droplet-grammar d channel-type s)
        parts (map (fn [[part _]] [:div.part [:h4 (name part)]
                                  (d/html (get-html-from-body (part (:content d)) (part (:envelope d))))]) grammar)]
    (ui/modal-dialog "full-droplet"
                     (apply conj [[:h3 (str (name channel-type) " droplet")]]
                            parts)
                     )))

(defn render-preview [droplet-address droplet-channel-scape s]
    (let [d ((:receptors s) droplet-address)
          channel-address (droplet-channel-scape droplet-address)
          channel-name (ssu/get-channel-name-from-address channel-address)
          channel-type (ssu/get-channel-type channel-address)
          channel-icon (ssu/channel-icon-html channel-name channel-type)
          sent (droplet-date s d :delivery-scape)
          from (resolve-ident s (:from d))
          ]
      [:div.droplet-preview
       (d/html channel-icon)
       [:div.preview-sent sent]
       [:div.preview-from from]
       [:div.preview-groove-specific (groove-preview d channel-type s)]
       (ui/make-click-link "Open" (fn [] (render-full d channel-type s)))
       ]))



(comment condp = channel-type
            :streamscapes :subject-body-message
            :email :subject-body-message
            :twitter :simple-message
            :irc :simple-message)
(defn get-droplet-grammar [d channel-type s]
  (let [dg (:values (:droplet-grooves-scape (:scapes s)))
        groove-name (first ((keyword (str (:address d))) dg ))
        ]
    (channel-type ((keyword groove-name) s/*grooves*))))

;;TODO: groove droplets should be auto-detected by some appropriate
;;programmatic method, not by channel-type!
(defn groove-preview [d channel-type s]
  (let [grammar (get-droplet-grammar d channel-type s)]
    (if (contains? grammar :subject)
      (str " Subject:" (:subject (:content d)))
      (str " : " (if (nil? (:text (:content d))) (:message (:content d)) (:text (:content d)))))))

(comment ui/make-zips (map (fn [da]
                        (let [d-addr (keyword da)
                              d ((:receptors s) d-addr)
                              channel-address (droplet-channel-scape d-addr)
                              channel (ssu/get-channel-name-from-address channel-address)
                              channel-type (ssu/get-channel-type channel-address)
                              ccc (first (ssu/get-matching-scapes-by-relationship #"droplet-address" #"boolean"))
                              ]

                          (condp = channel-type
                              :streamscapes (zip-for-streamscapes-droplet s d-addr channel)
                              :email (zip-for-email-droplet s d-addr channel)
                              :twitter {:title (d/html (str (ssu/channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-twitter-avatar s (:from d)) (resolve-ident s (:from d)) " : " (:text (:content d))))
                                        :content (d/build [:div [:div#default-droplet (u/clj->json (:content d)) ]
                                                           (ui/make-button (str ccc) #(categorize da ccc))]) }
                              :irc {:title (str (ssu/channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)) " : " (:message (:content d)))
                                    :content (d/build [:div [:div#default-droplet (u/clj->json (:content d)) ]]) }
                              {:title (str "Via:" (name channel) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)))
                               :content (d/build [:div [:div#default-droplet (u/clj->json (:content d)) ]]) }
                              ))) (:receptor-order s))
                 elem)
;; Actions

(defn categorize [droplet-address scape-name]
  (ssu/send-ss-signal {:aspect "scape" :signal "set"
                       :params {:name (descapify scape-name) :key droplet-address :address true}} refresh-stream-callback))
