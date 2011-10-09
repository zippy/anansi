(ns ss.streamscapes
  (:require [clojure.browser.dom :as dom]
            [clojure.string :as string]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.session :as s]
            [ss.dom-helpers :as d]
            [ss.ceptr :as ceptr]
            [ss.ui :as ui]
            [ss.ss-utils :as ssu]
            [ss.compose :as compose]
            ))

(defn refresh-stream []
  (get-state (s/get-ss-addr)))

(defn render-ss []
  (render-stream ssu/*current-state*)
  (render-scapes ssu/*current-state*)
  )

(defn gs-callback [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (ssu/set-current-state result)
    (try
      (d/remove-children :the-receptor)
      (render-receptor ssu/*current-state* (d/get-element :the-receptor) "")
      (render-ss)
      (catch js/Object e (debug/jslog e))
      (finally (ui/loading-end)))))

(defn get-state
  "get the state of a streamscapes receptor and render it"
  [r]
  (do
    (ui/loading-start)
    (ceptr/command {:cmd "get-state" :params {:receptor r :query {:scape-order {:scape :delivery :limit 40 :descending true}}}} gs-callback)))


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
    (dom/append elem (d/build [:div.stream-control
                               (ui/make-button "Compose" compose/compose)
                               (ui/make-button "Refresh" refresh-stream)
                               ]))
    (dom/append elem (d/build [:h3 (str "stream: " (count droplet-channel-scape) " of " (:receptor-total s))]))
    (ui/make-zips (map (fn [da]
                        (let [d-addr (keyword da)
                              d ((:receptors s) d-addr)
                              channel (droplet-channel-scape d-addr)
                              channel-type (get-channel-type channel)
                              ]

                          (condp = channel-type
                              :streamscapes {:title (str (channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)) " Subject: " (:subject (:content d)))
                                             :content (:body (:content d))}
                              :email (zip-for-email-droplet s d-addr channel)
                              :twitter {:title (str (channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)) " : " (:text (:content d)))
                                        :content (d/build [:div [:div#default-droplet (u/clj->json (:content d)) ]]) }
                              :irc {:title (str (channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)) " : " (:message (:content d)))
                                    :content (d/build [:div [:div#default-droplet (u/clj->json (:content d)) ]]) }
                              {:title (str "Via:" (name channel) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)))
                               :content (d/build [:div [:div#default-droplet (u/clj->json (:content d)) ]]) }
                              ))) (:receptor-order s))
                 elem)))

(defn render-scapes [s]
  (let [elem (d/get-element :ss-panel)
        scapes (:scapes s)]
    (d/remove-children :ss-panel)
    (dom/append elem (d/build [:h3 "streamscapes"]))
    (dom/append elem (d/build (apply conj
                                     [:div#channels [:h4 "channels"]]
                                     (map (fn [[cname caddr]]
                                            [:p (d/html (let [type (get-channel-type cname)]
                                                          (str (channel-icon-html cname type)
                                                               (name cname)
                                                               (if (= type :email)
                                                                 (str "<button onclick=\"ss.email.compose('" (name cname) "')\">Compose</button>")
                                                                 ""))))])
                                          (:values (:channel-scape scapes))))))
    (dom/append elem ())))

;; These are the functions for the debug rendering of the receptor

(defn build-scape-contents [s address]
  (cond (keyword? s) (name s)
        (or (nil? s) (empty? s) (= "" s)) ""
        (map? s) (let [{kr :key ar :address} (:relationship s)
                       napair (and (= kr "name") (= ar "address"))
                       anpair (and (= kr "address") (= ar "name"))
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
  (fn [e] (let [r (if (= address "") ssu/*current-state*
                     (get-receptor-by-address ssu/*current-state* (rest (string/split address #"\."))))]
           (d/remove-children :the-receptor)
           (render-receptor r (d/get-element :the-receptor) address))))

