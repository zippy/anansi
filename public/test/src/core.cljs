(ns test.core
  (:require [clojure.browser.dom :as dom]
            [clojure.string :as string]
            [test.dom-helpers :as tdom]
            [goog.ui.Prompt :as prompt]
            [goog.events :as events]
            [goog.style :as style]
            [goog.ui.LabelInput :as LabelInput]
            [goog.net.XhrIo :as xhr]
            [goog.debug.DebugWindow :as debugw]
            [goog.ui.Dialog :as dialog]
            [goog.ui.Button :as button]
            [test.debug :as debug]
            [goog.Uri :as uri]
            [goog.editor.Field :as field]
            [cljs.reader :as reader]
            [test.utils :as u]
            [test.makezip :as z]
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

(defn auth-callback [e]
  (let [{status :status result :result} (process-xhr-result e)]
    (def session (:session result))
    (tdom/set-text :session session)
    )
  )

(def auth (goog.ui.Prompt. "Authenticate" "User"
                           (fn [r]
                             (if (not ( nil? r))
                               (ceptr->command {:cmd "authenticate" :params {:user r}}  auth-callback)))))
(def new_user (goog.ui.Prompt. "New User" "User"
                           (fn [r]
                             (if (not ( nil? r))
                               (ceptr->command {:cmd "new-user" :params {:user r}}  test-callback)))))

(defn send-signal
  ([params] (send-signal params test-callback))
  ([params callback] (ceptr->command {:cmd "send-signal" :params (assoc params :session session)} callback))
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
    (render-receptor the-state (tdom/get-element :the-receptor) "")))

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
                                               [n _] (string/split scape #"-")
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

(defn get-state [r] (ceptr->command {:cmd "get-state" :params {:receptor r}} gs-callback))
(defn hidfn [address]
  (fn [e] (let [r (if (= address "") the-state
                     (get-receptor-by-address the-state (rest (string/split address #"\."))))]
           (tdom/remove-children :the-receptor)
           (render-receptor r (tdom/get-element :the-receptor) address))))



