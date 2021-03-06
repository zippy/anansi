(ns ss.state
  (:require
   [goog.net.cookies :as cookie]
   [goog.net.Cookies :as Cookie]         
   [clojure.string :as string]
   [ss.debug :as debug]
   [ss.utils :as u]))

(defn get-session []
  (.get goog.net.cookies "ss-session"))

(defn set-session [s]
  (.set goog.net.cookies "ss-session" s -1)
  )
(defn get-ss-addr []
  (js/parseInt (.get goog.net.cookies "ss-address")))

(defn set-ss-addr [sa]
  (.set goog.net.cookies "ss-address" sa -1))

(defn get-user-name [s]
  (.get goog.net.cookies "ss-user-name"))

(defn set-user-name [n]
  (.set goog.net.cookies "ss-user-name" n -1))

(defn clear-session []
  (def *current-state* nil)
  (def *grooves* nil)
  (def *me* nil)
  (def *page* 1)
  (clear-scape-query)
  (.remove goog.net.cookies "ss-session")
  (.remove goog.net.cookies "ss-address")
  (.remove goog.net.cookies "ss-user-name"))

(defn set-current-state
  "set the current streamscapes state for others to refer to it"
  [s]
  (def *current-state* s)
  )
(defn set-grooves
  "set the current streamscapes grooves"
  [g]
  (def *grooves* g)
  )

(defn get-groove-grammar [groove]
  (if (nil? groove) nil (-> *grooves* groove :grammar))
  )

(defn get-groove-preview [groove]
  (if (nil? groove) nil (-> *grooves* groove :preview))
  )

(defn get-groove-channel-actions [groove channel-type]
  (if (nil? groove) nil (-> *grooves* groove :carriers channel-type :actions))
  )

(defn set-me
  "store my contact address"
  [c]
  (def *me* c))

(defn get-my-contact-address []
  (-> *current-state* :scapes :sender-scape :values keys first name js/parseInt))

(defn set-scape-query [scape value]
  (def *scape-query* [scape value]))

(defn clear-scape-query []
  (def *scape-query* nil))

(defn set-page [page]
  (def *page* page)
  )

(def *items-per-page* 20)
(def *page* 1)

(defn get-offset []
  (* *items-per-page* (- *page* 1)))
