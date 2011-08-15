(ns
  #^{:author "Eric Harris-Braun"
     :doc "Host receptor"}
  anansi.receptor.host
  (:use [anansi.ceptr]
        [anansi.receptor.commons-room]
        [anansi.receptor.user]
        [anansi.receptor.scape]))

(defmethod manifest :host [_r]
           (make-scapes _r {} :room :user :stream))

;; TODO make this an generalized receptor host
;; (defmacro make-receptor [n p & a] `(receptor :~(symbol (str (name n))) ~p ~@a))
;; (defn do-make-receptor [n p & a] (receptor :'(symbol (str (name n))) p a))
(signal self host-room [_r _f {receptor-name :name password :password matrice-address :matrice-address data :data}]
        (rsync _r
         (let [names (get-scape _r :room)
               r (receptor :commons-room _r matrice-address password data) ;;(make-receptor type _r args)
               addr (address-of r)]
           (--> key->set _r names receptor-name addr)
           addr)))
(signal self host-streamscape [_r _f {receptor-name :name password :password matrice-address :matrice-address data :data}]
        (rsync _r
         (let [names (get-scape _r :stream)
               r (receptor :streamscapes _r matrice-address password data) ;;(make-receptor type _r args)
               addr (address-of r)]
           (--> key->set _r names receptor-name addr)
           addr)))
(signal self host-user [_r _f receptor-name]
        (rsync _r
         (let [names (get-scape _r :user)
               existing-addr (--> key->resolve _r names receptor-name)
               addr (if existing-addr existing-addr (address-of (receptor :user _r receptor-name nil))) ;; (make-receptor type _r args)                
               ]
           (--> key->set _r names receptor-name addr)
           addr)))
