(ns ss.ui
  (:require
   [clojure.browser.dom :as dom]
   [ss.debug :as d]
   [goog.ui.LabelInput :as LabelInput]
   [goog.editor.Field :as field]
   [goog.ui.Button :as uiButton]
   [goog.ui.Dialog :as uiDialog]
   [goog.ui.Dialog.ButtonSet :as uiDialogButtonSet]
   [goog.ui.Dialog.EventType :as uiDialogEventType]
   [goog.ui.ToggleButton :as uiToggleButton]
   [goog.ui.CustomButton :as uiCustomButton]
   [goog.ui.ButtonRenderer :as uiButtonRenderer]
   [goog.ui.FlatButtonRenderer :as uiFlatButtonRenderer]
   [goog.ui.LinkButtonRenderer :as uiLinkButtonRenderer]
   [goog.ui.CustomButtonRenderer :as uiCustomButtonRenderer]
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
        inputs (into [] (map (fn [{id :field l :label h :hint}]
                               (let [label (if (nil? l) (name id) l)]
                                 [id (goog.ui.LabelInput. h) label])) fields))
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

(defn make-input
  ([label id size]
     (make-input label id size nil))
  ([label id size default]
     [:p 
      [:label {:for id} (str label ":")]
      [(keyword (str "input#" id)) {:name id :size size :value default}]])
  )

(defn make-button
  ([text click-fun ]
     (make-button text click-fun false))
  ([text click-fun return-both]
      (let [button (goog.ui.Button. text (goog.ui.CustomButtonRenderer.getInstance))
            button-elem (d/element :span)
            ]
        (.render button button-elem)
        (goog.events.listen button-elem goog.events.EventType.CLICK click-fun)
        (if return-both [button button-elem] button-elem))))

(defn make-click-link
  [text click-fun]
  (let [link-elem (d/build [:a {:href "javascript:void(0)"} text])]
    (goog.events.listen link-elem goog.events.EventType.CLICK click-fun)
    link-elem))

(defn add-click-fun [element click-fun]
  (goog.events.listen element goog.events.EventType.CLICK click-fun)
  (d/add-class element "clickable")
  element)

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

(defn make-toggle-button
  ([text click-fun ]
     (make-button text click-fun false))
  ([text click-fun return-both]
      (let [button (goog.ui.ToggleButton. text)
            button-elem (d/element :span)
            ]
        (.render button button-elem)
        (goog.events.listen button-elem goog.events.EventType.CLICK click-fun)
        (if return-both [button button-elem] button-elem))))

(defn make-menu [name items]
  (let [elem (d/element :div.menu-container )
        menu (doto (goog.ui.Menu.) (.setId (str name "Menu")))
        button (goog.ui.MenuButton. name menu)]
    (doseq [[label callback] items]
      (.addItem menu (goog.ui.MenuItem. label)))
    (.render button elem)

    (goog.events.listen button goog.ui.Component.EventType.ACTION
      (fn [event]
        (let [ selected-label (. (.target event) (getCaption))
               callback (second (first (filter #(= selected-label (first %)) items)))]
         (callback))))
    elem))

(defn modal-dialog [id header-spec buildelems]
  (let [[header-text other-header-items] (if (string? header-spec) [header-spec []] [(first header-spec) (into [] (rest header-spec))])
        header-items (apply conj other-header-items [[:div.top-right-controls (make-button "Close" cancel-modal)] [:h3 header-text]])]
    (d/insert-at (d/get-element :everything)
                 (d/build [:div#modalmask.overlay-mask
                           [(keyword (str "div#" id ".standard-modal"))
                            (apply conj [:div.modal-header] header-items)
                            (apply conj [:div.modal-content] buildelems)]
                           ]) 0)))
(defn cancel-modal []
  (d/remove-node :modalmask))

;; Functions for creating zippys
(defn init
  [title content node-container]

  {:title title :content content :parent node-container})

(defn make-zippy-dom
  [self]

  (let [title (:title self)
        header-element  (d/build
                         [:div {:style "background-color:#EEE"}
                          (if (string? title) (d/html title) title)])
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

(defn make-confirm-dialog
  "creates an Yes/No confirm dialog"
  []
  (let [d (doto (goog.ui.Dialog.)
            (.setTitle "Please Confirm")
            (.setButtonSet goog.ui.Dialog.ButtonSet.OK_CANCEL))]
    d))

(defn confirm-dialog [text fun]
  (let [d (make-confirm-dialog)]
    (doto d
      (.setContent text)
      (.setVisible true))
    (goog.events.listen d goog.ui.Dialog.EventType.SELECT fun)))

(defn reset
  "resets the UI and the state to the basic non-logged in state"
  []
  (d/remove-children :the-receptor)
  (d/remove-children :debug-log)
  (d/remove-children :header-top-right)
  (s/clear-session)
  (d/hide :container)
  (d/show :authpane))
