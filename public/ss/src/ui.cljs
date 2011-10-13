(ns ss.ui
  (:require
   [clojure.browser.dom :as dom]
   [goog.ui.LabelInput :as LabelInput]
   [goog.editor.Field :as field]
   [goog.ui.Button :as uibutton]
   [goog.ui.Select :as uiselect]
   [goog.ui.Option :as uioption]
   [goog.ui.Component.EventType :as event-type]
   [goog.ui.Zippy :as zippy]
   [goog.events :as events]
   [ss.dom-helpers :as d]
   [ss.utils :as u]
   [ss.state :as s]
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
  [parent-id fun inputs]
  (fn [e]
    (let [values (into {} (map (fn [[id li]] [id (. li (getValue))] ) inputs))]
      (fun values)
      (d/remove-children parent-id))))

(defn make-dialog
  "creates a dialog based on the vector of field specs, and a function to call back if OK is pressed"
  [parent-id fields okfn]
  (let [e (d/get-element parent-id)
        inputs (into [] (map (fn [{id :field l :label}]
                               (let [label (if (nil? l) (name id) l)]
                                 [id (goog.ui.LabelInput. label) label])) fields))
        defaults (into {} (map (fn [{id :field default :default}] [id default]) fields))
        b (goog.ui.Button. "Submit")
        bc (goog.ui.Button. "Cancel")] 
    (d/remove-children parent-id)
    
    (doseq [[id li label] inputs]
      (d/append e (d/build [:div.field (d/element "label" {:for (name id)} label) (d/element "input" {:id (name id)})]))
      (.decorate li (d/get-element id))
      (.setValue li (id defaults))
      )
    (let [domb (d/element :span#submit-button)
          cancel (d/element :span#cancel-button)
          ]
      (d/append e (d/build [:div domb cancel]))
      (.render b domb) (.render bc cancel)
      (goog.events.listen domb goog.events.EventType.CLICK (make-ok-fn parent-id okfn inputs))
      (goog.events.listen cancel goog.events.EventType.CLICK (fn [e] (d/remove-children parent-id)))
      )
    (d/show parent-id)
    ))

(defn make-input [label id size]
  [:p 
   [:label {:for id} (str label ":")]
   [(keyword (str "input#" id)) {:name id :size size}]]
  )

(defn make-button
  ([text click-fun]
     (make-button text click-fun false))
  ([text click-fun return-both]
      (let [button (goog.ui.Button. text)
            button-elem (d/element :span)
            ]
        (.render button button-elem)
        (goog.events.listen button-elem goog.events.EventType.CLICK click-fun)
        (if return-both [button button-elem] button-elem))))

(defn make-select [elem-id caption options select-fun]
  (let [select (goog.ui.Select. caption)
        select-elem (d/element (keyword (str "span#" elem-id)))]
    (doseq [option options]
      (.addItem select (if (vector? option) (let [[caption value] option] (goog.ui.Option. caption value)) (goog.ui.Option. option)) ))
    (.render select select-elem)
    (goog.events.listen select goog.ui.Component.EventType.ACTION select-fun)
    [select select-elem]
    )
  )

(defn modal-dialog [id buildelems]
  (let [cbe (make-button "Close" cancel-modal)
        x (apply conj
                 [(keyword (str "div#" id ".standard-modal"))]
                 (apply conj [[:div.top-right-controls cbe]] buildelems))
        ]
    (d/insert-at (d/get-element :everything)
                 (d/build [:div#modalmask.overlay-mask
                           x]) 0)))
(defn cancel-modal []
  (d/remove-node :modalmask))

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

(defn reset
  "resets the UI and the state to the basic non-logged in state"
  []
  (d/remove-children :the-receptor)
  (d/remove-children :debug)
  (d/remove-children :header-top-right)
  (s/clear-session)
  (d/hide :container)
  (d/show :authpane))
