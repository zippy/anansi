(ns anansi.util
  (:use [clj-time.core :only [date-time]]))

;; Generic Utilities - FIXME move to utility file

(defn modify-keys
  "takes fn and map and returns new map with fn applied to keys"
  [tform m]
  (into {} (map (fn [[k v]] [ (tform k) v] ) m)))

(defn modify-vals
  "takes fn and map and returns new map with fn applied to vals"
  [tform m]
  (into {} (map (fn [[k v]] [ k (tform v)] ) m)))

(defn do-snapshot
  [mm l]
  (let [m (filter (fn [[k v]] (not= k :parent)) mm )]
    (into {} (map (fn [[k v]]
                    [k  (if (instance? clojure.lang.Ref v)
                          (let [ref-key (keyword (last (re-find #"@(.*)$" (str v))))]
                            (if (get l ref-key)
                              '(get-ref ~ref-key)
                              (if (map? @v)
                                {ref-key (do-snapshot @v (conj l ref-key))}
                                @v)))
                          v)]
                    ) m))))

(defn snapshot [m]
  "removes all refs from a map"
  (do-snapshot m #{}))

(defn javaDate2datetime [jd]
  (date-time (+ 1900 (.getYear jd)) (+ 1 (.getMonth jd)) (.getDate jd) (.getHours jd) (.getMinutes jd) (.getSeconds jd)))

(defn filter-map
  "returns the portion of the map that's described in the pattern
The pattern is a map of keys that match the desired keys in map, and values
which are true if the entire value from the source map is to be returned, or
another map of the same pattern format if the value in the map is also a map
that should be recursively filtered."
  [m pattern]
  (into {} (keep identity (map (fn [[k v]] (let [pv (k pattern)]
                                            (cond
                                             (map? pv) [k (filter-map v pv)]
                                             (= pv true) [k v]
                                             true nil))) m))))
