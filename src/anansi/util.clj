(ns anansi.util)

;; Generic Utilities - FIXME move to utility file

(defn modify-keys
  "takes fn and map and returns new map with fn applied to keys"
  [tform m]
  (into {} (map (fn [[k v]] { (tform k) v }) m)))

(defn get-first
  "returns first non-nil value from looking up keys in the-map"
  [the-map & keys]
  (first (filter (complement nil?) (map #(get the-map %) keys))))


