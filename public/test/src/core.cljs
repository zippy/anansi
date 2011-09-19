(ns test.core
  (:require [clojure.browser.dom :as dom]
            [test.dom-helpers :as tdom]
            [goog.ui.Prompt :as prompt]
            [goog.events :as events]
            [goog.ui.LabelInput :as LabelInput]
            [goog.net.XhrIo :as xhr]
            [goog.debug.DebugWindow :as debugw]
            [test.debug :as debug]
            [goog.Uri :as uri]
            [cljs.reader :as reader]
            [test.utils :as u]
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

(defn send-signal [params] (ceptr->command {:cmd "send-signal" :params (assoc params :session session)} test-callback))

(defn ping [] (send-signal {:to 0 :prefix "receptor.host" :aspect "ceptr" :signal "ping"}))
