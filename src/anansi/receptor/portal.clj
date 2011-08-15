(ns
  #^{:author "Eric Harris-Braun"
     :doc "Portal receptor"}
  anansi.receptor.portal
  (:use [anansi.ceptr]))

(defmethod manifest :portal [_r & [target-addr]]
    {:target (if (nil? target-addr) 0 target-addr)} )
(defmethod state :portal [_r full?]
           (assoc (state-convert _r full?)
             :target (contents _r :target)))
(defmethod restore :portal [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :target (:target state))
             r))

(signal self enter [_r _f unique-name occupant-data]
        (receptor :occupant
                  (let [t-addr (contents _r :target)]
                    (if (= t-addr 0) (parent-of _r) (get-receptor _r t-addr)))
                  unique-name occupant-data)
        )
