(ns
  #^{:author "Eric Harris-Braun"
     :doc "Droplet receptor"}
  anansi.streamscapes.droplet
  (:use [anansi.ceptr]))

(defmethod manifest :droplet [_r id from to aspect envelope content]
           {:id id :from from :to to :aspect aspect :envelope envelope :content content})
(defmethod state :droplet [_r full?]
           (assoc (state-convert _r full?)
             :id (contents _r :id)
             :from (contents _r :from)
             :to (contents _r :to)
             :aspect (contents _r :aspect)
             :envelope (contents _r :envelope)
             :content (contents _r :content)
             ))
(defmethod restore :droplet [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :id (:id state))
             (restore-content r :from (:from state))
             (restore-content r :to (:to state))
             (restore-content r :aspect (:aspect state))
             (restore-content r :envelope (:envelope state))
             (restore-content r :content (:content state))
             r))
