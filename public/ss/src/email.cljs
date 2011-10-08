(ns ss.email
  (:require [clojure.string :as string]
            [ss.debug :as debug]
            [ss.utils :as u]
            [ss.dom-helpers :as d]
            [ss.ceptr :as ceptr]
            [ss.ui :as ui]
            ))

(defn compose []
  (ui/modal-dialog "compose-email"
                   [[:h3 "Compose Email"]
                    [:input "x"]]
                   ))
