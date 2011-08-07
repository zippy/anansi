(ns
  #^{:author "Eric Harris-Braun"
     :doc "Identity receptor"}
  anansi.streamscapes.ident
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]))

(defmethod manifest :ident [_r {name :name}]
           {:name name})
(defmethod state :ident [_r full?]
           (assoc (state-convert _r full?)
             :name (contents _r :name)
             ))
(defmethod restore :ident [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :name (:name state))
             r))
