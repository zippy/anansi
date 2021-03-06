(ns ss.ceptr
  (:require 
            [clojure.string :as string]
            [goog.events :as events]
            [goog.Uri :as uri]
            [goog.net.XhrIo :as xhr]
            [ss.debug :as debug]
            [ss.utils :as u]
            ))

(def ceptr-url (goog.Uri. "/api"))

(defn command
  "Send command to a ceptr host via it's http interface"
  [cmd callback]
  (let [payload (u/clj->json cmd)]
    (xhr/send ceptr-url callback "POST" payload))
)

(defn handle-xhr [e]
  "convert the result of the xhr call to a clojure map"
  (let [xhr (.target e)
        json (. xhr (getResponseJson))
        r (js->clj json :keywordize-keys true)
        {status :status result :result} r]
    (debug/log (str "CMD RESULT--Status: " status " Result: " (if (map? result) (u/clj->json result)  result)))
    r))

(defn null-signal-callback [e]
  (let [{status :status result :result} (handle-xhr e)]))

(defn signal
  ([params] (signal params null-signal-callback))
  ([params callback] (command {:cmd "send-signal" :params params } callback))
)

(defn ping [] (signal {:to 0 :prefix "receptor.host" :aspect "ceptr" :signal "ping"}))

(defn start-chain
  [chain]
  (let [first-callback (nextc chain)]
    (first-callback nil)
    )
  )
(defn nextc [{cleanup-fun :cleanup err-fun :error chain :chain}]
  (let [ok-fun (first chain)]
    (fn [e]
      (let [{status :status result :result} (if e (handle-xhr e) {:status "ok"})]
        (if (= status "ok")
          (if (nil? ok-fun)
            (cleanup-fun)
            (ok-fun result {:cleanup cleanup-fun :error err-fun :chain (rest chain)}))
          (do (cleanup-fun)
              (err-fun result)))))
    ))

