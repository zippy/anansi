(ns ss.makezip
  (:require [ss.dom-helpers :as dom]
            [goog.ui.Zippy :as zippy]))

(defn init
  [title content node-container]

  {:title title :content content :parent node-container})

(defn make-zippy-dom
  [self]

  (let [header-element  (dom/build
                         [:div {:style "background-color:#EEE"}
                          (:title self)])
        content-element (dom/build
                         [:div (:content self)])
        new-zip (dom/build
                  [:div header-element content-element])
        zippy (goog.ui.Zippy. header-element content-element)]

      (dom/append (:parent self)
              new-zip)
      ;; Return an updated self object with the above declarations
      (-> self
          (assoc :header-element header-element)
          (assoc :content-element content-element)
          (assoc :zippy zippy))))

(defn make-zips
  [data node-container]
  (doseq [cont data]
    (let [self
          (init (:title cont) (:content cont) node-container)]
      (make-zippy-dom self))))
