;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ss.dom-helpers
  (:require [clojure.string :as string]
            [goog.dom :as dom]
            [goog.style :as style]
            ))

(defn get-element
  "Return the element with the passed id."
  [id]
  (dom/getElement (name id)))

(defn append
  "Append all children to parent."
  [parent & children]
  (do (doseq [child children]
        (dom/appendChild parent child))
      parent))

(defn set-text
  "Set the text content for the passed element returning the
  element. If a keyword is passed in the place of e, the element with
  that id will be used and returned."
  [e s]
  (let [e (if (keyword? e) (get-element e) e)]
    (doto e (dom/setTextContent s))))

(defn normalize-args [tag args]
  (let [parts (string/split (name tag) #"(\.|#)")
        [tag attrs] [(first parts)
                     (apply hash-map (map #(cond (= % ".") :class
                                                 (= % "#") :id
                                                 :else %)
                                          (rest parts)))]
        attrs (if (contains? attrs :class) (assoc attrs :class (string/replace (:class attrs) "_" " ")) attrs)
        ]
    
    (if (map? (first args))
      [tag (merge attrs (first args)) (rest args)]
      [tag attrs args])))

;; TODO: replace call to .strobj with whatever we come up with for
;; creating js objects from Clojure maps.

(defn element
  "Create a dom element using a keyword for the element name and a map
  for the attributes. Append all children to parent. If the first
  child is a string then the string will be set as the text content of
  the parent and all remaining children will be appended."
  [tag & args]
  (let [[tag attrs children] (normalize-args tag args)
        parent (dom/createDom (name tag)
                              (.strobj (reduce (fn [m [k v]]
                                                 (assoc m k v))
                                               {}
                                               (map #(vector (name %1) %2)
                                                    (keys attrs)
                                                    (vals attrs)))))
        [parent children] (if (string? (first children))
                            [(set-text (element tag attrs) (first children))
                             (rest children)]
                            [parent children])]
    (apply append parent children)))

(defn remove-children
  "Remove all children from the element with the passed id."
  [id]
  (let [parent (dom/getElement (name id))]
    (do (dom/removeChildren parent))))

(defn replace-children
  "replace the children of an element with an element"
  [id & new-children]
  (remove-children id)
  (apply append (get-element id) new-children)
)

(defn remove-node
  "Remove the element with the passed id."
  [id]
  (let [elem (dom/getElement (name id))]
    (do (dom/removeNode elem))))

(defn html
  "Create a dom element from an html string."
  [s]
  (dom/htmlToDocumentFragment s))

(defn- element-arg? [x]
  (or (keyword? x)
      (map? x)
      (string? x)))

(defn build
  "Build up a dom element from nested vectors."
  [x]
  (if (vector? x)
    (let [[parent children] (if (keyword? (first x))
                              [(apply element (take-while element-arg? x))
                               (drop-while element-arg? x)]
                              [(first x) (rest x)])
          children (map build children)]
      (apply append parent children))
    x))

(defn insert-at
  "Insert a child element at a specific location."
  [parent child index]
  (dom/insertChildAt parent child index))

(defn vis
  "set the visibility of an element by id"
  [elem val]
  (style/showElement (get-element elem) val))

(defn show
  "show an element by id"
  [elem] (vis elem true))

(defn hide
  "hide an element by id"
  [elem] (vis elem false))
