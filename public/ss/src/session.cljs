(ns ss.session
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
(defn set-ss-addr [sa]
  (.set goog.net.cookies "ss-address" sa -1))

(defn get-ss-addr []
  (js/parseInt (.get goog.net.cookies "ss-address")))

(defn clear-session []
  (.remove goog.net.cookies "ss-session")
  (.remove goog.net.cookies "ss-address"))
