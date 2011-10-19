(ns ss.core
  (:require [clojure.browser.dom :as dom]
            [cljs.reader :as reader]
            [clojure.browser.repl :as repl]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.ceptr :as ceptr]
            [ss.state :as s]
            [ss.dom-helpers :as d]
            [ss.streamscapes :as sss]
            [ss.ss-utils :as ssu]
            [ss.auth :as auth]
            [ss.ui :as ui]
            [ss.droplet :as droplet]
            ))


(defn make-ss []
  (ui/make-dialog {:name ""}
               (fn [params]
                 (ssu/send-signal {:to 0 :prefix "receptor.host" :aspect "self" :signal "host-streamscape" :params (merge {:matrice-address 999} params)})
                 )))

(defn on-load []
  (auth/check_auth)
  (repl/connect "http://localhost:9000/repl")
  )

