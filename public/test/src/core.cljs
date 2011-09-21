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
        true
        (u/clj->json s)))

(defn gs-callback [e]
  (let [{status :status result :result} (process-xhr-result e)]
    (debug/log (str "State:" (u/clj->json result)))
    (tdom/remove-children :scapes)    (tdom/remove-children :receptors)

    (let [scapes  (into  [] (map (fn [[k v]]
                                   (let [scape (name k)
                                         [n _] (string/split scape #"-")
                                         ]
                                     {:title (str n "s") :content (build-scape-contents v)})) (:scapes result)))
          receptors (into  [] (map (fn [[raddr r]]
                                   (let [rtype (:type r)]
                                     {:title (str rtype "-" (name raddr)) :content (build-receptor-contents r)})) (filter (fn [[k v]] (not= k :last-address)) (:receptors result))
                                     ))
          ]
      (z/make-zips scapes (dom/get-element :scapes))
      (z/make-zips receptors (dom/get-element :receptors))
      )))

(defn get-state [] (ceptr->command {:cmd "get-state" :params {:receptor 0}} gs-callback))
