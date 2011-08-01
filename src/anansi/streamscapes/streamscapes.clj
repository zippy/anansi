(ns
  #^{:author "Eric Harris-Braun"
     :doc "Streamscapes receptor"}
  anansi.streamscapes.streamscapes
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  )

(defmethod manifest :streamscapes [_r matrice-address password data]
           (let [ms (receptor scape _r)]
             (s-> key->set ms matrice-address :matrice)
             (make-scapes _r  {:password password
                               :matrice-scape ms
                               :data data
                               }
                          :aspect
                          )))

(defmethod state :streamscapes [_r full?]
           (let [base-state (state-convert _r full?)]
             (if full?
               (assoc base-state
                 :password (contents _r :password)
                 :matrice-scape (address-of (contents _r :matrice-scape))
                 :data (contents _r :data)
                 )
               (assoc base-state 
                 :data (contents _r :data)
                 :matrices (s-> key->all (contents _r :matrice-scape))
                   )))
           )
(defmethod restore :streamscapes [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :password (:password state))
             (restore-content r :matrice-scape (get-receptor r (:matrice-scape state)))
             (restore-content r :data (:data state))
             r))

(signal matrice incorporate [_r _f {from :from to :to aspect :aspect envelope :envelope content :content}]
        (rsync _r
               (let [d (receptor droplet _r from to aspect envelope content)
                     addr (address-of d)
                     aspects (contents _r :aspect-scape)
                     ]
                 (--> key->set _r aspects addr aspect)
                 addr)))
