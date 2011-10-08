(ns ss.ui
  (:require
   [clojure.browser.dom :as dom]
   [goog.ui.LabelInput :as LabelInput]
   [goog.editor.Field :as field]
   [goog.ui.Button :as button]
   [goog.ui.Zippy :as zippy]
   [goog.events :as events]
   [ss.dom-helpers :as d]
    ))

(defn loading-start
  "displays the loading overlay screen"
  []
  (d/insert-at (d/get-element :everything) (d/build [:div#loading.overlay-mask [:p "LOADING..."]]) 0)
  )
(defn loading-end
  "hides the loading overlay screen"
  []
  (d/remove-node :loading))

(defn make-ok-fn
  "utility function to make an ok button function which clears the dialog"
  [fun inputs]
  (fn [e]
    (let [values (into {} (map (fn [[id li]] [id (. li (getValue))] ) inputs))]
      (fun values)
      (d/remove-children :work))))

(defn make-dialog
  "creates a dialog based on the hash map given, and a function to call back if OK is pressed"
  [spec okfn]
  (let [e (d/get-element :work)
        inputs (into {} (map (fn [[id label]] [id (goog.ui.LabelInput. (name id))]) spec))
        b (goog.ui.Button. "Submit")
        bc (goog.ui.Button. "Cancel")
        build-vec (into [:form] (map (fn [[id li]] (goog.dom.createDom "label" {"for" (name id)}) ) inputs))] ;(keyword (str "input#" (name id)))
    (d/remove-children :work)
                                        ;(dom/append e (d/build build-vec))
;    (dom/append e (d/element "cow" {:style "color:red"} "dog"))
    (doseq [[id li] inputs]
      (dom/append e (d/element "label" {:for (name id)} (name id)) (d/element "input" {:id (name id)}))
      (.decorate li (d/get-element id))
      (.setValue li (id spec))
      )
    (let [domb (d/element :span#submit-button)
          cancel (d/element :span#cancel-button)
          ]
      (dom/append e domb) (.render b domb)
      (dom/append e cancel) (.render bc cancel)
      (goog.events.listen domb goog.events.EventType.CLICK (make-ok-fn okfn inputs))
      (goog.events.listen cancel goog.events.EventType.CLICK (fn [e] (d/remove-children :work)))
      )
    ))

;; Functions for creating zippys
(defn init
  [title content node-container]

  {:title title :content content :parent node-container})

(defn make-zippy-dom
  [self]

  (let [header-element  (d/build
                         [:div {:style "background-color:#EEE"}
                          (d/html (:title self))])
        content-element (d/build
                         [:div (:content self)])
        new-zip (d/build
                  [:div header-element content-element])
        zippy (goog.ui.Zippy. header-element content-element)]

      (d/append (:parent self)
              new-zip)
      ;; Return an updated self object with the above declarations
      (-> self
          (assoc :header-element header-element)
          (assoc :content-element content-element)
          (assoc :zippy zippy))))

(defn make-zips
  "creates a bunch of zippys from a vector of hashes of the form [{:title <title element> :content <content element>}...]"
  [data node-container]
  (doseq [cont data]
    (let [self
          (init (:title cont) (:content cont) node-container)]
      (make-zippy-dom self))))

