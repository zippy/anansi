(ns ss.ss-utils
  (:require [ss.state :as s]
            [ss.debug :as debug]
            [ss.ceptr :as ceptr]
            [ss.utils :as u]
            [ss.ui :as ui]
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
  [key-pattern address-pattern]
  (map (fn [[scape-name _]] scape-name) (filter (fn [[_ {{k :key a :address} :relationship}]]
                                                 (and (re-find key-pattern k) (re-find address-pattern a))) (:scapes s/*current-state*))))
(defn get-matching-scapes-by-relationship-key
  "returns the scape names who's relationship key values match a pattern"
  [pattern]
  (get-matching-scapes-by-relationship pattern #".*"))

(defn get-matching-scapes-by-relationship-address
  "returns the scape names who's relationship address values match a pattern"
  [pattern]
    (get-matching-scapes-by-relationship #".*" pattern))

(defn get-channel-type [channel-address]
  (let [type ((keyword (str channel-address)) (:values (:channel-type-scape (:scapes s/*current-state*))))]
    (keyword type)))

(defn get-channel-address-from-name [channel-name]
  ((keyword channel-name) (:values (:channel-scape (:scapes s/*current-state*))))
  )

;;TODO: note that this assumes a one-to-one relationship in the
;;channel-scape, which, though appropriate, is not declared and is
;;part of the work that needs to be done in deepening scape semantics
(defn get-channel-name-from-address [channel-address]
  (first (u/get-keys (:values (:channel-scape (:scapes s/*current-state*))) channel-address))
  )

(defn get-channel-type-from-name [channel-name]
  (get-channel-type (get-channel-address-from-name channel-name)))

(defn get-channel-types
  "return a list of the current channel types"
  []
  (map (fn [t] (keyword t)) (vals (:values (:channel-type-scape (:scapes s/*current-state*)))))
  )

;;TODO: really we should be able to get the ident-scape
;;programmatically instead of manually like this.
(defn get-channel-ident-scape-from-type
  "Given a channel type, returns the identity scape for that channel type"
  [chan-type]
  (:values ((condp = chan-type
                  :streamscapes :ss-address-ident-scape
                  :irc :irc-ident-scape
                  :email :email-ident-scape
                  :twitter :twitter-ident-scape
                  ) (:scapes s/*current-state*))))

(defn get-channel-ident-scape
  "Given a channel name, returns the identity scape for that channel"
  [channel-name]
  (get-channel-ident-scape-from-type (get-channel-type-from-name channel-name)))

(defn channel-icon-html [channel-name channel-type]
  (str "<img class=\"droplet-type-icon\" src=\"images/" (name channel-type) ".png\" title=\"" (name channel-name) "\">")
  )

(defn get-droplet-tags [droplet-address]
  (let [scapes (:scapes s/*current-state*)]
    (into [] (filter (fn [scape] (contains? (:values ((keyword (str scape "-scape")) scapes)) droplet-address)) (keys (:values (:tag-scapes-scape scapes)))))))

(defn tag-droplet [droplet-address tag-scape]
  (send-ss-signal {:aspect "scape" :signal "set"
                       :params {:name tag-scape :key (js/parseInt (name droplet-address)) :address true}} ss.streamscapes/refresh-stream-callback))

(defn make-tagging-button [droplet-address]
  (ui/make-menu "Tags"
                (into [] (map (fn [[sn tn]] [tn #(tag-droplet droplet-address sn)])
                              (:values (:tag-scapes-scape (:scapes s/*current-state*)))))))
