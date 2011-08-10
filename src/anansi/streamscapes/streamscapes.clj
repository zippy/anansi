(ns
  #^{:author "Eric Harris-Braun"
     :doc "Streamscapes receptor"}
  anansi.streamscapes.streamscapes
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.streamscapes.ident])
  )

(defmethod manifest :streamscapes [_r matrice-address password data]
           (let [ms (receptor scape _r)]
             (s-> key->set ms matrice-address :matrice)
             (make-scapes _r  {:password password
                               :matrice-scape ms
                               :data data
                               }
                          :aspect :id :email-ident :ident-name
                          )))

(defmethod state :streamscapes [_r full?]
           (let [base-state (state-convert _r full?)]
             (if full?
               (assoc base-state
                 :password (contents _r :password)
                 :matrice-scape (address-of (get-scape _r :matrice))
                 :data (contents _r :data)
                 )
               (assoc base-state 
                 :data (contents _r :data)
                 :matrices (s-> key->all (get-scape _r :matrice))
                   )))
           )
(defmethod restore :streamscapes [state parent]
           (let [r (do-restore state parent)]
             (restore-content r :password (:password state))
             (restore-content r :matrice-scape (get-receptor r (:matrice-scape state)))
             (restore-content r :data (:data state))
             r))

(defn do-incorporate
  "add a droplet receptor into the streamscape"
  [_r _f {id :id from :from to :to aspect :aspect envelope :envelope content :content}]
  (rsync _r
         (let [d (receptor droplet _r id from to aspect envelope content)
               addr (address-of d)
               aspects (get-scape _r :aspect)
               ids (get-scape _r :id)
               ]
           (--> key->set _r aspects addr aspect)
           (--> key->set _r ids addr id)
           addr)))

(defn do-identify
  "add an identity receptor into the streamscape, scaping the email and name appropriately"
  ([_r params] (do-identify _r params true))
  ([_r {email :email name :name} throw-if-exists]
     (let [email-idents (get-scape _r :email-ident)
           iaddr (--> key->resolve _r email-idents email)
           exists (not (nil? iaddr))]
           
       (if (and exists throw-if-exists)
         (throw (RuntimeException. (str "identity for " email " already exists")))
         (rsync _r
                (let [iname (if (nil? name) (str "name for " email) name)
                      ident-address (if exists iaddr (address-of (receptor ident _r {:name iname})))
                      ident-names (get-scape _r :ident-name)]
                  (--> key->set _r email-idents email ident-address)
                  (--> key->set _r ident-names ident-address iname)
                  ident-address))))))

(signal channel incorporate [_r _f params]
        ; TODO add in authentication to make sure that _f is one of this
        ; streamscape instance's channels
        (do-incorporate _r _f params))

(signal matrice incorporate [_r _f params]
        ; TODO add in authentication to make sure that _f is a matrice
        (do-incorporate _r _f params))

(signal matrice identify [_r _f params]
        ; TODO add in authentication to make sure that _f is a matrice
        (do-identify _r params))

