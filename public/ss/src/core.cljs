(ns ss.core
  (:require [clojure.browser.dom :as dom]
            [clojure.string :as string]
            [ss.dom-helpers :as tdom]
            [goog.ui.Prompt :as prompt]
            [goog.events :as events]
            [goog.style :as style]
            [goog.ui.LabelInput :as LabelInput]
            [goog.net.XhrIo :as xhr]
            [goog.debug.DebugWindow :as debugw]
            [goog.ui.Dialog :as dialog]
            [goog.ui.Button :as button]
            [goog.net.cookies :as cookie]
            [goog.net.Cookies :as Cookie]
            [ss.debug :as debug]
            [goog.Uri :as uri]
            [goog.editor.Field :as field]
            [cljs.reader :as reader]
            [ss.utils :as u]
            [ss.makezip :as z]
            ))

(def ceptr-url (goog.Uri. "http://localhost:8080/api"))

(defn ll [str]
    (js* "console.log(~{str})"))

(defn ceptr->command
  "Send command to ceptr."
  [cmd callback]
  (let [payload (u/clj->json cmd)]
    (ll (str "about to send:" payload))
    (xhr/send ceptr-url callback "POST" payload))
)

;(def debug (goog.debug.DebugWindow.))
(comment doto debug (.addLogRecord (goog.debug.LogRecord. goog.debug.Logger.Level.INFO "messge" "source"))
         
        (.setVisible true))

(defn process-xhr-result [e]
  "convert the result of the xhr call to a clojure map"
  (let [xhr (.target e)
        json (. xhr (getResponseJson))
        r (js->clj json :keywordize-keys true)
        {status :status result :result} r]
    (debug/log (str "CMD RESULT--Status: " status " Result: " result))
    r))

(defn test-callback [e]
  (let [{status :status result :result} (process-xhr-result e)]
    )
  )

(defn set-session [s]
  (tdom/set-text :session s)
  (.set goog.net.cookies "ss-session" s -1)
)

(defn get-session []
  (.get goog.net.cookies "ss-session"))

(defn clear-session []
  (.remove goog.net.cookies "ss-session"))

(defn do-logged-in [session]
  (do (set-session session)
      (hide :authpane)
      (show :container)
      (refresh-stream)))

(defn do-logged-out []
  (do
    (tdom/remove-children :the-receptor)
    (tdom/remove-children :debug)
    (clear-session)
    (hide :container)
    (show :authpane))
  )

(defn auth-callback [e]
  (let [{status :status result :result} (process-xhr-result e)]
    (if (= status "ok")
      (do-logged-in result)
      (do (js/alert result)
          (do-logged-out)))))

(defn do-auth [] (.setVisible auth true))
(def auth (goog.ui.Prompt. "Authenticate" "User"
                           (fn [r]
                             (if (not ( nil? r))
                               (ceptr->command {:cmd "authenticate" :params {:user r}}  auth-callback)))))

(defn new-user-callback [e]
  (let [{status :status result :result} (process-xhr-result e)]
    (if (= status "ok")
      (do (js/alert "Thanks! Now you need to authenticate.")
          (do-auth))
      (js/alert result))))

(defn do-new-user [] (.setVisible new-user true))
(def new-user (goog.ui.Prompt. "New User" "User"
                               (fn [r]
                             (if (not ( nil? r))
                               (ceptr->command {:cmd "new-user" :params {:user r}}  new-user-callback)))))

(defn send-signal
  ([params] (send-signal params test-callback))
  ([params callback] (ceptr->command {:cmd "send-signal" :params (assoc params :session (get-session))} callback))
)

(defn ping [] (send-signal {:to 0 :prefix "receptor.host" :aspect "ceptr" :signal "ping"}))

(defn build-receptor-contents [r]
  (u/clj->json r))

(defn make-ok-fn [fun inputs]
  (fn [e]
    (let [values (into {} (map (fn [[id li]] [id (. li (getValue))] ) inputs))]
      (fun values)
      (tdom/remove-children :work))))
(defn make-irc [params]
  (send-signal {:to 8 :prefix "streamscapes.streamscapes" :aspect "setup" :signal "new-channel"
                :params (merge {:type :irc, :name :freenode} params)})
  )
(defn make-email [p]
  (let [params {:type :email :name (:channel-name p)
                :in {:host (:in-host p) :account (:in-account p) :password (:in-password p) :protocol (:in-protocol p)}
                :out {:host (:out-host p) :account (:out-account p) :password (:out-password p) :protocol (:out-protocol p) :port (:out-port p)}}]
    (send-signal {:to 8 :prefix "streamscapes.streamscapes" :aspect "setup" :signal "new-channel"
                  :params params}))
  )

(defn make-dialog [spec okfn]
  (let [e (tdom/get-element :work)
        inputs (into {} (map (fn [[id label]] [id (goog.ui.LabelInput. (name id))]) spec))
        b (goog.ui.Button. "Submit")
        bc (goog.ui.Button. "Cancel")
        build-vec (into [:form] (map (fn [[id li]] (goog.dom.createDom "label" {"for" (name id)}) ) inputs))] ;(keyword (str "input#" (name id)))
    (tdom/remove-children :work)
                                        ;(dom/append e (tdom/build build-vec))
;    (dom/append e (tdom/element "cow" {:style "color:red"} "dog"))
    (doseq [[id li] inputs]
      (dom/append e (tdom/element "label" {:for (name id)} (name id)) (tdom/element "input" {:id (name id)}))
      (.decorate li (tdom/get-element id))
      (.setValue li (id spec))
      )
    (let [domb (tdom/element :span#submit-button)
          cancel (tdom/element :span#cancel-button)
          ]
      (dom/append e domb) (.render b domb)
      (dom/append e cancel) (.render bc cancel)
      (goog.events.listen domb goog.events.EventType.CLICK (make-ok-fn okfn inputs))
      (goog.events.listen cancel goog.events.EventType.CLICK (fn [e] (tdom/remove-children :work)))
      )
    ))
(defn make-email-channel []
  (make-dialog
   {:channel-name "email" :in-host "mail.harris-braun.com" :in-account "eric@harris-braun.com" :in-password "pass" :in-protocol "pop3"
    :out-host "mail.harris-braun.com" :out-account "eric@harris-braun.com" :out-password "pass" :out-protocol "smtps" :out-port 25}
   make-email
   ))

(defn refresh-stream-callback [e]
  (let [{status :status result :result} (process-xhr-result e)]
    (refresh-stream)
    )
  )
(defn twitter-check [c]
  (send-signal {:to 8 :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "control-channel"
                :params {:name c :command :check} }
               refresh-stream-callback)
  )

(defn make-twitter [p]
  (let [screen-name (:twitter-name p)
        params {:type :twitter :name (str "twitter-" screen-name) :screen-name screen-name}]
    (send-signal {:to 8 :prefix "streamscapes.streamscapes" :aspect "setup" :signal "new-channel"
                  :params params}))
  )

(defn make-twitter-channel []
  (make-dialog
   {:twitter-name "zippy314"}
   make-twitter      
   )
  )

(defn email-check []
  (send-signal {:to 8 :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "control-channel"
                :params {:name :email :command :check} }
               refresh-stream-callback))

(defn make-irc-channel []
  (make-dialog {:host "irc.freenode.net", :port 6667, :user "Eric", :nick "zippy31415"}
             make-irc       
             ))
(defn make-ss []
  (make-dialog {:name ""}
               (fn [params]
                 (send-signal {:to 0 :prefix "receptor.host" :aspect "self" :signal "host-streamscape" :params (merge {:matrice-address 7} params)})
                 )))
(defn irc-join []
  (make-dialog {:channel "#ceptr"}
               (fn [params]
                 (send-signal {:to 8 :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "control-channel"
                               :params {:name :freenode :command :join :params params} }))))
(defn irc-open []
  (send-signal {:to 8 :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "control-channel"
                :params {:name :freenode :command :open}}))
(defn irc-close []
  (send-signal {:to 8 :prefix "streamscapes.streamscapes" :aspect "matrice" :signal "control-channel"
                  :params {:name :freenode :command :close}}))

(defn vis [elem val]
  (style/showElement (tdom/get-element elem) val))
(defn show [elem] (vis elem true))
(defn hide [elem] (vis elem false))

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
                     (tdom/build x)
                     (catch js/Object e (ll (u/clj->json x))))
                   )
        true (u/clj->json s)))

(defn build-scape-title [n s]
  (let [{v :values {kr :key ar :address} :relationship} s]
    (str n "s (" (count v) ")" (if (nil? kr) " no-rel-defined" (str " " (name kr) "->" (name ar))))))


(defn gs-callback [e]
  (let [{status :status result :result} (process-xhr-result e)]
    (def the-state result)
    (debug/log (str "State:" (u/clj->json result)))
    (tdom/remove-children :the-receptor)
    (render-receptor the-state (tdom/get-element :the-receptor) "")
    (rs)))

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
    (dom/append relem (tdom/element "h2" (str "Receptor: " (name fingerprint) " @ " address)))
    (if (= fingerprint "anansi.streamscapes.droplet.droplet")
      (dom/append relem (tdom/build [:div#drop [:h3 "Droplet"] [:div.content (u/clj->json (:content state))] [:div.envelope (u/clj->json (:envelope state))]])))
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
          
        (dom/append relem (tdom/build [:div#scapes [:h3 "Scapes"]]))
        (z/make-zips scapes-vec (dom/get-element :scapes))
        )
      (dom/append relem (tdom/build [:div#scapes [:h3 "No Scapes"]])))
    (if (> (count receptors) 0)
      (do
        (dom/append relem (tdom/build [:div#receptors [:h3 "Receptors"]]))        
        (doseq [[r-addr r] receptors]
          (let [raddr (str address "." (name r-addr) )
                html-id (keyword (str "r-" raddr))]
            
            (add-receptor-button (dom/get-element :receptors) raddr (str (:fingerprint r) "@" raddr)))))
      (dom/append relem (tdom/build [:div#receptors [:h3 "No Sub Receptors"]])))))

(defn add-receptor-button [parent raddr text]
  (let [
        elem (tdom/build [:div.rbutton [:x text]])]

    (if (not (nil? parent)) (dom/append parent elem))
    (goog.events.listen elem goog.events.EventType.CLICK, (hidfn raddr))
    elem))

(defn get-receptor-by-address [state addr-list]
  (let [a (keyword (first addr-list))
        r (a (:receptors state))
        ]
    (if (= 1 (count addr-list))
      r
      (get-receptor-by-address r (rest addr-list)))
    )
  )

(defn refresh-stream []
  (get-state 8))

(defn get-state [r] (ceptr->command {:cmd "get-state" :params {:receptor r :query {:scape-order {:scape :delivery :limit 40 :descending true}}}} gs-callback))

(defn hidfn [address]
  (fn [e] (let [r (if (= address "") the-state
                     (get-receptor-by-address the-state (rest (string/split address #"\."))))]
           (tdom/remove-children :the-receptor)
           (render-receptor r (tdom/get-element :the-receptor) address))))

(defn rs []
  (let [s the-state]
    (render-stream s)
    (render-scapes s))
  )

(defn humanize-ss-datetime [dt]
  (let [[date ltime] (string/split (name dt) #"T")
        [year month day] (string/split date #"-")
        [time _] (string/split ltime #"\.")
        [hour min sec] (string/split time #":")
        ]
    (str month "/" day "/" year " " hour ":" min )
      )
  )
;;TODO: iniffecient, this scans the receipt scape every time, we should have an inverse lookup...
(defn droplet-date [s d scape]
  (let [r (:values (scape (:scapes s)))
        a (:address d)
        [[t _]] (remove (fn [[_ address]] (not (= address a))) r)
        ]
    (humanize-ss-datetime t)
    )
  )

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
        html (tdom/build [:div (tdom/html (get-html-from-body body content-type))])
        ]
    (z/make-zips [{:title "Raw" :content (u/clj->json body)}] html)
    {:title (str (channel-icon-html channel :email) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)) " Subject: " (:subject (:content d)))
     :content (tdom/build [:div [:div#html html]])}
    )
  )

;; TODO: this a terrible cheat in that we determine droplet type just by scanning
;; the channel name!  This should be fixed to get the type from the
;; channel receptor.
(defn get-channel-type [channel]
  (cond (re-find #"email" channel) :email
        (re-find #"twitter" channel) :twitter
        (re-find #"irc|freenode" channel) :irc
        true :generic
        ))

(defn channel-icon-html [channel-name channel-type]
  (str "<img class=\"droplet-type-icon\" src=\"images/" (name channel-type) ".png\" title=\"" (name channel-name) "\">")
  )

(defn render-stream [s]
  (let [elem (tdom/get-element :stream-panel)
        scapes (:scapes s)
        droplet-channel-scape (:values (:droplet-channel-scape scapes))
        ]
    (tdom/remove-children :stream-panel)
    (dom/append elem (tdom/html "<div class=\"stream-control\"><button onclick=\"ss.core.refresh_stream()\"> Refresh </button></div>"))
    (dom/append elem (tdom/build [:h3 (str "stream: " (count droplet-channel-scape) " of " (:receptor-total s))]))
    (z/make-zips (map (fn [da]
                        (let [d-addr (keyword da)
                              d ((:receptors s) d-addr)
                              channel (droplet-channel-scape d-addr)
                              channel-type (get-channel-type channel)
                              ]

                          (condp = channel-type
                              :email (zip-for-email-droplet s d-addr channel)
                              :twitter {:title (str (channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)) " : " (:text (:content d)))
                                        :content (tdom/build [:div [:div#default-droplet (u/clj->json (:content d)) ]]) }
                              :irc {:title (str (channel-icon-html channel channel-type) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)) " : " (:message (:content d)))
                                    :content (tdom/build [:div [:div#default-droplet (u/clj->json (:content d)) ]]) }
                              {:title (str "Via:" (name channel) " Sent: " (droplet-date s d :delivery-scape) " From: " (resolve-ident s (:from d)))
                               :content (tdom/build [:div [:div#default-droplet (u/clj->json (:content d)) ]]) }
                              ))) (:receptor-order s))
                 elem)))

(defn short-fingerprint [r]
  (:fingerprint r)
  )
(defn render-scapes [s]
  (let [elem (tdom/get-element :ss-panel)
        scapes (:scapes s)]
    (tdom/remove-children :ss-panel)
    (dom/append elem (tdom/build [:h3 "streamscapes"]))
    (dom/append elem (tdom/build (apply conj
                                        [:div#channels [:h4 "channels"]]
                                        (map (fn [[cname caddr]] [:p (tdom/html (str (channel-icon-html cname (get-channel-type cname)) (name cname)))]) (:values (:channel-scape scapes))))))
    (dom/append elem ())
    )
  )

(defn check-auth []
  (let [s (get-session)]
    (if (or (= s js/undefined) (nil? s)) (do-auth) (refresh-stream))))
