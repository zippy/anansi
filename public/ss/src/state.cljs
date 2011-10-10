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
  (.remove goog.net.cookies "ss-session")
  (.remove goog.net.cookies "ss-address")
  (.remove goog.net.cookies "ss-user-name"))

(defn set-current-state
  "set the current streamscapes state for others to refer to it"
  [s]
  (def *current-state* s)
  )
