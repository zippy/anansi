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
            [ss.stream :as stream]
            [ss.ss-utils :as ssu]
            ))

(defn refresh-stream-callback
  "a signal a callback for simply reloading the state from the server"
  [e]
  (let [{status :status result :result} (ceptr/handle-xhr e)]
    (refresh-current-stream)))

(defn refresh-current-stream []
  (let [[scape value] s/*scape-query*]
    (refresh-stream scape value)))

(defn refresh-stream
  ([] (refresh-stream nil nil))
  ([scape value] (get-state (s/get-ss-addr) scape value)))

(defn render-ss []
  (stream/render #(refresh-stream nil nil))
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
  "get the state of a streamscapes receptor and render it, possibly limmiting the receptors returned to those in a given scape"
  ([r] (get-state r nil nil))
  ([r scape value] 
     (let [q {:scape-order {:scape :delivery :limit 40 :descending true :frequencies true}}
           query (if (nil? scape) q (assoc q :scape-query {:scape scape  :query ["=" value] :flip true}))]
       (if (nil? scape)
         (s/clear-scape-query)
         (s/set-scape-query scape value)
         )
       (ui/loading-start)
       (ceptr/command {:cmd "get-state" :params {:receptor r :query query}} gs-callback))))

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

(defn descapify [sn]
  (string/join "-" (reverse (rest (reverse (string/split (name sn) #"-")))))
  )

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

(defn humanize-scape-name-for-list [sn]
  ;; way ugly but drops the ending "-scape"
  (str "by " (string/join " " (reverse (rest (reverse (string/split (name sn) #"-"))))))
  )

(defn get-groove-scapes []
  (map (fn [sn]
         (let [scape (descapify sn)]
           [(scape-tag scape true)
            (ui/make-click-link (humanize-scape-name-for-list sn) #(refresh-stream scape true))])) (ssu/get-matching-scapes #"-groove-scape$")))

(defn get-tag-scapes []
  (let [scapes (:scapes s/*current-state*)]
    (map (fn [[scape-name tag-name]]
           [(scape-tag scape-name true)
            (ui/make-click-link tag-name #(refresh-stream scape-name true))])
         (:values (:tag-scapes-scape scapes)))))

(defn get-order-scapes []
  (map (fn [sn] [:p (humanize-scape-name-for-list sn)]) (ssu/get-matching-scapes-by-relationship-address #"droplet-address"))
  )

(defn get-category-scapes
  "produces UI for filtering by categories that are defined by two scapes,
one that maps names onto some linking value, an the other that maps droplet-addresses
onto the linking value."
  []
  ;; for now the categor-name-scapes are hard-coded, but later they will be
  ;; pulled dynamically from the scape definition (or from another scape!)
  (let [category-scapes []  ;[:channel-scape :droplet-channel-scape]
        scapes (:scapes s/*current-state*)
        ]
    (map (fn [[name-scape address-scape]]
           (apply conj [:div.category [:h5 (humanize-scape-name-for-list name-scape)]]
                  (map (fn [[cat-name v]] [:p (ui/make-click-link (name cat-name) #(refresh-stream (descapify address-scape) v))
                                          ]) (:values (name-scape scapes)))) ) category-scapes)
    ))
;; This code is for showing all scapes that map droplet-addresses...
 (comment let [key-scapes (ssu/get-matching-scapes-by-relationship-key #"droplet-address")
        scapes (:scapes s)]
    (map (fn [sn] (apply conj [:div.key-scape [:h5 (humanize-scape-name-for-list sn)]] (map (fn [[_ v]] [:p (name v)]) (:values (sn scapes))))) key-scapes))


(defn make-scape-section [section-name contents]
  (apply conj [:div.section] [:h4 section-name]  contents)
  )

(defn scape-tag [scape value]
  (let [[qscape qval] s/*scape-query*]
       (if (and (= qscape scape) (= qval value)) :p.active :p)))

(defn get-channel-buttons [type cname]
  (cond
    (or (= type :email ) (= type :twitter ))
    [(ui/make-menu "G" [["Check" #(channel-check cname)]])]
    (= type :irc )
    [(ui/make-menu "G"
        [["Open", #(irc-open cname)]
         ["Close", #(irc-close cname)]
         ["Join", #(irc-join cname)]])]
    true []))

(defn render-scapes [s]
  (let [elem (d/get-element :scape-panel )
        scapes (:scapes s)
        ]
    (d/remove-children :scape-panel)
    (dom/append elem (d/build [:div
                               (make-scape-section "channels"
                                                   (map (fn [[cname caddr]]
                                                          (let [tag (scape-tag :droplet-channel caddr)]
                                                            (apply conj [tag] (let [type (ssu/get-channel-type caddr)]
                                                                               (apply conj
                                                                                      [(d/html (ssu/channel-icon-html cname type))]
                                                                                      [(ui/make-click-link (name cname) #(refresh-stream :droplet-channel caddr))]
                                                                                      (get-channel-buttons type cname))))))
                                                        (:values (:channel-scape scapes))))
                               (make-scape-section "grooves" (get-groove-scapes))
                               (make-scape-section "tags" (get-tag-scapes))
;                               (make-scape-section "ordering scapes" (get-order-scapes))
;                               (make-scape-section "categorizing scapes" (get-category-scapes))
                               ]))
;    (dom/append elem ())
    ))

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


