(ns test.core
  (:require [clojure.browser.dom :as dom]
            [clojure.string :as string]
            [test.dom-helpers :as tdom]
            [goog.ui.Prompt :as prompt]
            [goog.events :as events]
            [goog.ui.LabelInput :as LabelInput]
            [goog.net.XhrIo :as xhr]
            [goog.debug.DebugWindow :as debugw]
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
    (def session result)
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

(defn make-ss [] (send-signal {:to 0 :prefix "receptor.host" :aspect "self" :signal "host-streamscape" :params {:name "zippy" :matrice-address 7}}))

(defn build-receptor-contents [r]
  (u/clj->json r))

(defn build-scape-contents [s]
  (cond (keyword? s) (name s)
        (or (nil? s) (empty? s) (= "" s)) ""
        (map? s) (let [{kr :key ar :address} (:relationship s)
                       napair (and (= kr "name") (= ar "address"))
                       anpair (and (= kr "address") (= ar "name"))
                       x (into [:div.scape-items]
                           (map (fn [[k v]]
                                  (if (map? v)
                                    [:div.scape-item [:h3 (name k)] (into [:ul] (map (fn [[kk vv]] [:li (str (name kk) ": " (str vv))]) v))]
                                    [:div.scape-item (cond napair (add-receptor-button nil v (str (name k) " (@" v ")"))
                                                           anpair (add-receptor-button nil k (str (name v) " (@" k ")"))
                                                           t [:h3 (str "key:" (str k) " val:" (str v))]
                                                       )
                                     [:h3 ]])) (:values s)))]; 
                   (tdom/build x))
        true (u/clj->json s)))

(defn build-scape-title [n s]
  (let [{v :values {kr :key ar :address} :relationship} s]
    (str n "s (" (count v) ")" (if (nil? kr) " no-rel-defined" (str " " (name kr) "->" (name ar))))))

(defn gs-callback [e]
  (let [{status :status result :result} (process-xhr-result e)
        relem (tdom/get-element :the-receptor)
        ]
    (debug/log (str "State:" (u/clj->json result)))
    (tdom/remove-children :the-receptor
                           )
    (dom/append relem (tdom/element "h2" (str "Receptor: " (name (:type result)) " @ " (:address result))))
    (let [scapes (:scapes result)
          receptors (:receptors result)
          ]
      (if (> (count scapes) 0)
        (let [scapes-vec  (into  [] (map (fn [[k s]]
                                           (let [scape (name k)
                                                 [n _] (string/split scape #"-")
                                                 ]
                                             {:title (build-scape-title n s) :content (build-scape-contents s)})) scapes))
              x (comment into  [] (map (fn [[raddr r]]
                                         (let [rtype (:type r)]
                                           {:title (str rtype "@" (name raddr)) :content (build-receptor-contents r)})) (filter (fn [[k v]] (not= k :last-address)) receptors)
                                           ))
              ]
          (tdom/get-element :the-receptor)
          (dom/append relem (tdom/build [:div#scapes [:h3 "Scapes"]]))
          (z/make-zips scapes-vec (dom/get-element :scapes))
          )
        (dom/append relem (tdom/build [:div#scapes [:h3 "No Scapes"]])))
      (if (> (count receptors) 0)
        (do 
          (dom/append relem (tdom/build [:div#receptors [:h3 "Receptors"]]))        
          (doseq [[r-addr r] (filter (fn [[k v]] (not= k :last-address)) (:receptors result))]
            (let [raddr (name r-addr)
                  html-id (keyword (str "r-" raddr))]
              (add-receptor-button (dom/get-element :receptors) raddr (str (:type r) "@" raddr))
              )
            ))))))

(defn add-receptor-button [parent raddr text]
  (let [
        elem (tdom/build [:div.rbutton [:x text]])]

    (if (not (nil? parent)) (dom/append parent elem))
    (goog.events.listen elem goog.events.EventType.CLICK, (hidfn (js/parseInt raddr)))
    elem))

(defn get-state [r] (ceptr->command {:cmd "get-state" :params {:receptor r}} gs-callback))
(defn hidfn [hid]
  (fn [e] (get-state hid)))



