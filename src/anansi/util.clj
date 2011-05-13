(ns anansi.util)

;; Generic Utilities - FIXME move to utility file

(defn modify-keys
  "takes fn and map and returns new map with fn applied to keys"
  [tform m]
  (into {} (map (fn [[k v]] [ (tform k) v] ) m)))

(defn modify-vals
  "takes fn and map and returns new map with fn applied to vals"
  [tform m]
  (into {} (map (fn [[k v]] [ k (tform v)] ) m)))

(defn- do-snapshot
  [m l]
  (into {} (map (fn [[k v]]
                  [k  (if (instance? clojure.lang.Ref v)
                        (if (or (= k :parent) (get l (str v)))
                          :skipping
                          (if (map? @v)
                            (do-snapshot @v (conj l (str v)))
                            @v))
                        v)]
                  ) m)))

(defn snapshot [m]
  "removes all refs from a map"
  (do-snapshot m #{}))

