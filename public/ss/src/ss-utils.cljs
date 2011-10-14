(ns ss.ss-utils
  (:require [ss.state :as s]
            [ss.debug :as debug]
            [ss.ceptr :as ceptr]
            ))

(defn send-signal
  "send signal to the host, inserting the current session into the params"
  ([params] (ceptr/signal (assoc params :session (s/get-session))))
  ([params callback] (ceptr/signal (assoc params :session (s/get-session)) callback)))


(defn send-ss-signal
  "send signal to the current session streamscapes instance"
  ([params] (ceptr/signal (assoc params :session (s/get-session) :to (s/get-ss-addr) :prefix "streamscapes.streamscapes")))
  ([params callback] (ceptr/signal (assoc params :session (s/get-session) :to (s/get-ss-addr) :prefix "streamscapes.streamscapes") callback)))

(defn get-matching-scapes
  "returns the scape names who's name match a pattern"
  [pattern]
  (filter (fn [scape] (re-find pattern (name scape))) (keys (:scapes s/*current-state*))))

(defn get-matching-scapes-by-relationship
  "returns the scape names who's relationship values match a pattern"
  [pattern]
  (map (fn [[scape-name _]] scape-name) (filter (fn [[_ {{k :key a :address} :relationship}]]
                                                 (or (re-find pattern k) (re-find pattern a))) (:scapes s/*current-state*))))
(defn get-matching-scapes-by-relationship-key
  "returns the scape names who's relationship key values match a pattern"
  [pattern]
  (map (fn [[scape-name _]] scape-name) (filter (fn [[_ {{k :key a :address} :relationship}]]
                                                 (re-find pattern k) ) (:scapes s/*current-state*))))
(defn get-matching-scapes-by-relationship-address
  "returns the scape names who's relationship address values match a pattern"
  [pattern]
  (map (fn [[scape-name _]] scape-name) (filter (fn [[_ {{k :key a :address} :relationship}]]
                                                 (re-find pattern a)) (:scapes s/*current-state*))))
