(ns ss.debug
  (:require [ss.dom-helpers :as d]
;            [goog.debug.DebugWindow :as debugw]                       
            ))

(defn log [txt]
  (d/insert-at (d/get-element :debug) (d/build [:div#thelog.logdiv [:div.logmsg txt]]) 0)
  )

(defn jslog [txt]
  (js* "console.log(~{txt})")
  )

;(def debug (goog.debug.DebugWindow.))
(comment doto debug (.addLogRecord (goog.debug.LogRecord. goog.debug.Logger.Level.INFO "messge" "source"))
        (.setVisible true))

