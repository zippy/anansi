(ns
  #^{:author "Eric Harris-Braun"
     :doc "Utility functions that work on maps"}
  anansi.map-utilities
  )

(defn remove-values
  "Utility function that remvoes all entries in a map by value"
  [the-map values]
  (apply dissoc the-map (map (fn [[key _]] key) (filter (fn [[key val]] (some #{val} values)) the-map)))
  )

(defn remove-value
  "Utility function that remvoes all entries in a map by value"
  [the-map value]
  (remove-values the-map [value]))

(defn get-keys
  "Utility function to return a list of all the keys in a map that have a given value"
  [the-map value]
  (into [] (keep (fn [[key val]] (if (= val value) key nil)) the-map)))
