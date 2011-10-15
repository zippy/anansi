(ns ss.streamscapes
  (:require [clojure.browser.dom :as dom]
            [clojure.string :as string]
            [goog.ui.Prompt :as prompt]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.state :as s]
            [ss.dom-helpers :as d]
            [ss.ceptr :as ceptr]
            [ss.ui :as ui]
            [ss.droplet :as droplet]
            [ss.ss-utils :as ssu]
            ))

(defn refresh-stream-callback
  "a signal a callback for simply reloading the state from the severver"
  [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (refresh-stream)))

(defn refresh-stream []
  (get-state (s/get-ss-addr)))

(defn render-ss []
  (render-stream s/*current-state*)
  (render-scapes s/*current-state*)
  )

(defn gs-callback [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (s/set-current-state result)
    (try
      (if (= status "ok")
        (do 
          (d/remove-children :the-receptor)
          (render-receptor s/*current-state* (d/get-element :the-receptor) "")
          (render-ss))
        (do 
          (if (re-find #"unknown receptor:" result)
            (ui/reset)
            (js/alert (str "Server responded with " result)))))
      (catch js/Object e (debug/jslog e))
      (finally (ui/loading-end)))
    ))

(defn get-state
  "get the state of a streamscapes receptor and render it"
  [r]
  (do
    (ui/loading-start)
    (ceptr/command {:cmd "get-state" :params {:receptor r :query {:scape-order {:scape :delivery :limit 40 :descending true}}}} gs-callback)))

(defn get-grooves
  "get the current groove definitions from the server" []
  (ceptr/command {:cmd "get-state" :params {:receptor 0 :query {:partial {:receptor-order true :scapes {:groove-scape {:values true}} :receptors true}
                                                                :scape-order {:scape :groove :scape-receptors-only true}}}}
                 (fn [e] (let [{status :status result :result} (ceptr/handle-xhr e)]
                          (if (= status "ok")
                            (let [receptors (:receptors result)
                                  grooves (into {} (map (fn [[groove-name addr]] [groove-name (:grammars ((keyword addr) receptors))]) (-> result :scapes :groove-scape :values)))]
                              (s/set-grooves grooves))
                            )
                          )) )
  )

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

;;TODO: this relies currently on channel type, which is not really an
;;ontological entitiy in the server, except in-so-far as grooves are
;;currently defined on the channel type key.  That all needs to be
;;fixed

(defn get-channel-ident-scape-from-type
  "Given a channel type, returns the identity scape for that channel type"
  [chan-type]
  (:values ((condp = chan-type
                  :streamscapes :ss-address-ident-scape
                  :irc :irc-ident-scape
                  :email :email-ident-scape
                  :twitter :twitter-ident-scape
                  ) (:scapes s/*current-state*))))

(defn get-channel-ident-scape
  "Given a channel name, returns the identity scape for that channel"
  [channel-name]
  (get-channel-ident-scape-from-type (get-channel-type channel-name)))

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
    {:title (str (channel-icon-html channel :email) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)) " Subject: " (:subject (:content d)))
     :content (d/build [:div [:div#html html]])}))

(defn zip-for-streamscapes-droplet [s d-addr channel]
  (let [d ((:receptors s) d-addr)
        body (:body (:content d))
        subject (:subject (:content d))
        message (:message (:content d))
        channel-type :streamscapes
        ]
    {:title (str (channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d))
                 (if (nil? subject) (str " : " message) (str " Subject: " subject)))
     :content (if (nil? body) "No Body" body)}
))


;; TODO: this a terrible cheat in that we determine droplet type just by scanning
;; the channel name!  This should be fixed to get the type from the
;; channel receptor.
(defn get-channel-type [channel]
  (cond (re-find #"email" channel) :email
        (re-find #"twitter" channel) :twitter
        (re-find #"irc|freenode" channel) :irc
        (re-find #"streamscapes" channel) :streamscapes
        true :generic
        ))

(defn channel-icon-html [channel-name channel-type]
  (str "<img class=\"droplet-type-icon\" src=\"images/" (name channel-type) ".png\" title=\"" (name channel-name) "\">")
  )

(defn render-stream [s]
  (let [elem (d/get-element :stream-panel)
        scapes (:scapes s)
        droplet-channel-scape (:values (:droplet-channel-scape scapes))
        ]
    (d/remove-children :stream-panel)
        (d/append elem
              (d/build [:div.stream-control
                        (ui/make-button "Create Droplet" droplet/create)
                        (ui/make-button "Refresh" refresh-stream)
                        ])
              (d/build [:h3 (str "stream: " (count droplet-channel-scape) " of " (:receptor-total s))]))

    (ui/make-zips (map (fn [da]
                        (let [d-addr (keyword da)
                              d ((:receptors s) d-addr)
                              channel (droplet-channel-scape d-addr)
                              channel-type (get-channel-type channel)
                              ccc (first (ssu/get-matching-scapes-by-relationship #"droplet-address" #"boolean"))
                              ]

                          (condp = channel-type
                              :streamscapes (zip-for-streamscapes-droplet s d-addr channel)
                              :email (zip-for-email-droplet s d-addr channel)
                              :twitter {:title (d/html (str (channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-twitter-avatar s (:from d)) (resolve-ident s (:from d)) " : " (:text (:content d))))
                                        :content (d/build [:div [:div#default-droplet (u/clj->json (:content d)) ]
                                                           (ui/make-button (str ccc) #(categorize da ccc))]) }
                              :irc {:title (str (channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)) " : " (:message (:content d)))
                                    :content (d/build [:div [:div#default-droplet (u/clj->json (:content d)) ]]) }
                              {:title (str "Via:" (name channel) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)))
                               :content (d/build [:div [:div#default-droplet (u/clj->json (:content d)) ]]) }
                              ))) (:receptor-order s))
                 elem)))

(defn descapify [sn]
  (string/join "-" (reverse (rest (reverse (string/split (name sn) #"-")))))
  )

(defn categorize [droplet-address scape-name]
  (ssu/send-ss-signal {:aspect "scape" :signal "set"
                       :params {:name (descapify scape-name) :key droplet-address :address true}} refresh-stream-callback))

(defn channel-check [c]
  (ssu/send-ss-signal {:aspect "matrice" :signal "control-channel"
                       :params {:name c :command :check} }
                      refresh-stream-callback))

(defn irc-join [c]
  (let [p (goog.ui.Prompt. "Join IRC Channel" "Channel"
                           (fn [irc-chan]
                             (if (not (nil? irc-chan))
                               (do
                                 (ssu/send-ss-signal {:aspect "matrice" :signal "control-channel"
                                                      :params {:name c :command :join :params {:channel irc-chan}} })))))]
    (.setVisible p true)
    ))

(defn irc-open [c]
  (ssu/send-ss-signal {:aspect "matrice" :signal "control-channel"
                :params {:name c :command :open}}))
(defn irc-close [c]
  (ssu/send-ss-signal {:aspect "matrice" :signal "control-channel"
                       :params {:name c :command :close}}))

(defn get-channel-buttons [type cname]
  (cond
   (or (= type :email) (= type :twitter)) [(ui/make-button "Check" #(channel-check cname))]
   (= type :irc) [(ui/make-button "Open" #(irc-open cname))
                  (ui/make-button "Close" #(irc-close cname))
                  (ui/make-button "Join" #(irc-join cname))]
   true []))

(defn humanize-scape-name-for-list [sn]
  ;; way ugly but drops the ending "-scape"
  (str "by " (string/join " " (reverse (rest (reverse (string/split (name sn) #"-"))))))
  )

(defn get-order-scapes []
  (map (fn [sn] [:p (humanize-scape-name-for-list sn)]) (ssu/get-matching-scapes-by-relationship-address #"droplet-address"))
  )


(defn get-category-scapes []
  ;; for now the categor-name-scapes are hard-coded, but later they will be
  ;; pulled dynamically from the scape definition (or from another scape!)
  (let [category-name-scapes [:channel-scape]
        scapes (:scapes s/*current-state*)
        ]
    (map (fn [scape] (apply conj [:div.category [:h5 (humanize-scape-name-for-list scape)]] (map (fn [[k _]] [:p (name k)]) (:values (scape scapes)))) ) category-name-scapes)
    ))
 (comment let [key-scapes (ssu/get-matching-scapes-by-relationship-key #"droplet-address")
        scapes (:scapes s)]
    (map (fn [sn] (apply conj [:div.key-scape [:h5 (humanize-scape-name-for-list sn)]] (map (fn [[_ v]] [:p (name v)]) (:values (sn scapes))))) key-scapes))


(defn make-scape-section [section-name contents]
  (apply conj [:div.section] [:h4 section-name]  contents)
  )

(defn render-scapes [s]
  (let [elem (d/get-element :ss-panel)
        scapes (:scapes s)
        ]
    (d/remove-children :ss-panel)
    (dom/append elem (d/build [:div
                               (make-scape-section "channels"
                                                   (map (fn [[cname caddr]]
                                                          (apply conj [:p] (let [type (get-channel-type cname)]
                                                                             (apply conj [(d/html (str (channel-icon-html cname type) (name cname)))]
                                                                                    (get-channel-buttons type cname)))))
                                                        (:values (:channel-scape scapes))))
                               (make-scape-section "ordering scapes" (get-order-scapes))
                               (make-scape-section "categorizing scapes" (get-category-scapes))
                               ]))
    (dom/append elem ())))

;; These are the functions for the debug rendering of the receptor

(defn build-scape-contents [s address]
  (cond (keyword? s) (name s)
        (or (nil? s) (empty? s) (= "" s)) ""
        (map? s) (let [{kr :key ar :address} (:relationship s)
                       napair (and (re-find #"-name$" kr) (re-find #"-address$" ar))
                       anpair (and (re-find #"-address$" kr) (re-find #"-name$" ar))
                       x (into [:div.scape-items]
                           (map (fn [[k v]]
                                  (if (map? v)
                                    [:div.scape-item [:h3 (name k)] (into [:ul] (map (fn [[kk vv]] [:li (str (name kk) ": " (str vv))]) v))]
                                    [:div.scape-item (cond napair (add-receptor-button nil (str address "." v)  (str (name k) " (@" v ")"))
                                                           anpair (add-receptor-button nil (str address "." k) parent-address (str (name v) " (@" k ")"))
                                                           true [:h3 (str "key:" (str k) " val:" (str v))]
                                                       )
                                     [:h3 ]])) (:values s)))]
                   (try 
                     (d/build x)
                     (catch js/Object e (debug/jslog (u/clj->json x))))
                   )
        true (u/clj->json s)))

(defn build-scape-title [n s]
  (let [{v :values {kr :key ar :address} :relationship} s]
    (str n "s (" (count v) ")" (if (nil? kr) " no-rel-defined" (str " " (name kr) "->" (name ar))))))

(defn build-receptor-contents [r]
  (u/clj->json r))

(defn parent-address [address]
  (string/join "." (reverse (rest (reverse (string/split address #"\."))))))

(defn render-receptor [state relem address]
  (let [scapes (:scapes state)
        receptors (into {} (filter (fn [[k v]] (not= k :last-address)) (:receptors state)))
        fingerprint (:fingerprint state)
        ]
    (if (not= address "")
      (let [pa (parent-address address)]
        (add-receptor-button relem pa (str "Parent-@" pa))))
    (dom/append relem (d/element "h2" (str "Receptor: " (name fingerprint) " @ " address)))
    (if (= fingerprint "anansi.streamscapes.droplet.droplet")
      (dom/append relem (d/build [:div#drop [:h3 "Droplet"] [:div.content (u/clj->json (:content state))] [:div.envelope (u/clj->json (:envelope state))]])))
    (if (> (count scapes) 0)
      (let [scapes-vec  (into  [] (map (fn [[k s]]
                                         (let [scape (name k)
                                               n (string/join "-" (reverse (rest (reverse (string/split scape #"-")))))
                                               ]
                                           {:title (build-scape-title n s) :content (build-scape-contents s address)})) scapes))
            x (comment into  [] (map (fn [[raddr r]]
                                       (let [rtype (:fingerprint r)]
                                         {:title (str rtype "@" (name raddr)) :content (build-receptor-contents r)})) (filter (fn [[k v]] (not= k :last-address)) receptors)
                                         ))
            ]
          
        (dom/append relem (d/build [:div#scapes [:h3 "Scapes"]]))
        (ui/make-zips scapes-vec (dom/get-element :scapes))
        )
      (dom/append relem (d/build [:div#scapes [:h3 "No Scapes"]])))
    (if (> (count receptors) 0)
      (do
        (dom/append relem (d/build [:div#receptors [:h3 "Receptors"]]))        
        (doseq [[r-addr r] receptors]
          (let [raddr (str address "." (name r-addr) )
                html-id (keyword (str "r-" raddr))]
            
            (add-receptor-button (dom/get-element :receptors) raddr (str (:fingerprint r) "@" raddr)))))
      (dom/append relem (d/build [:div#receptors [:h3 "No Sub Receptors"]])))))

(defn add-receptor-button [parent raddr text]
  (let [
        elem (d/build [:div.rbutton [:x text]])]

    (if (not (nil? parent)) (dom/append parent elem))
    (goog.events.listen elem goog.events.EventType.CLICK, (hidfn raddr))
    elem))

(defn get-receptor-by-address [state addr-list]
  (let [a (keyword (first addr-list))
        r (a (:receptors state))
        ]
    (if (= 1 (count addr-list))
      r
      (get-receptor-by-address r (rest addr-list)))))

(defn hidfn [address]
  (fn [e] (let [r (if (= address "") s/*current-state*
                     (get-receptor-by-address s/*current-state* (rest (string/split address #"\."))))]
           (d/remove-children :the-receptor)
           (render-receptor r (d/get-element :the-receptor) address))))

(defn get-channel-names
  "return a list of names of the current channels"
  []
  (map (fn [[cn _]] (name cn)) (:values (:channel-scape (:scapes s/*current-state*))))
  )

(defn get-channel-types
  "return a list of the current channel types"
  []
  (distinct (map (fn [cn] (get-channel-type cn)) (get-channel-names)))
  )
