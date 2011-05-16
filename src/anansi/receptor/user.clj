(ns
  #^{:author "Eric Harris-Braun"
     :doc "User receptor"}
  anansi.receptor.user
  (:use [anansi.ceptr]))

(defmethod manifest :user [_r name stream]
           {:name name
            :stream stream})
(defmethod state :user [_r full?]
           (assoc (state-convert _r full?)
             :name (contents _r :name)))
(defmethod restore :user [state parent]
           (let [r (do-restore state parent)]
             (set-content r :name (:name state))
             r))

(signal self disconnect [_r _f]
        (set-content _r :stream nil))

(signal self connect [_r _f stream]
        (set-content _r :stream stream))
