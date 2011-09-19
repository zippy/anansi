(ns test.debug
  (:require [test.dom-helpers :as dom]
            ))

(defn log [txt]
  (dom/insert-at (dom/get-element :debug) (dom/build [:div.logdiv [:div.logmsg txt]]))
  )
