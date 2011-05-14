(ns
  #^{:author "Eric Harris-Braun"
     :doc "Host receptor"}
  anansi.receptor.host
  (:use [anansi.ceptr]
        [anansi.receptor.commons-room]
        [anansi.receptor.user]
        [anansi.receptor.scape]))

(defmethod manifest :host [_r]
           (make-scapes _r {} :room :user))

;; TODO make this an generalized receptor host
(comment defmacro make-receptor [n p & a] `(receptor ~(symbol (str (name n))) ~p ~@a))
(comment defn do-make-receptor [n p & a] (receptor '(symbol (str (name n))) p a))
(signal self host-room [_r _f {receptor-name :name password :password matrice-address :matrice-address}]
        (dosync
         (let [names (contents _r :room-scape)
               r (receptor commons-room _r password matrice-address) ;;(make-receptor type _r args)
               addr (address-of r)]
           (--> key->set _r names receptor-name addr)
           addr)))
(signal self host-user [_r _f receptor-name]
        (dosync
         (let [names (contents _r :user-scape)
               existing-addr (--> key->resolve _r names receptor-name)
               addr (if existing-addr existing-addr (address-of (receptor user _r receptor-name nil))) ;; (make-receptor type _r args)                
               ]
           (--> key->set _r names receptor-name addr)
           addr)))
