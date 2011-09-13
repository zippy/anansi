(ns
  #^{:author "Eric Harris-Braun"
     :doc "Host receptor"}
  anansi.receptor.host
  (:use [anansi.ceptr]
        [anansi.receptor.commons-room]
        [anansi.receptor.user]
        [anansi.receptor.scape]
        [anansi.lib.sha :only [sha]])
  (:use [clj-time.core :only [now]]))

(defmethod manifest :host [_r]
           (make-scapes _r {} :room :user :stream :session))

(defn resolve-name [_r user]
  "resolve a username to it's receptor address"
  (let [ names (get-scape _r :user)]
    (--> key->resolve _r names user)))

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

(signal interface send-signal [_r _f {from-user :from to-addr :to signal :signal params :params}]
        (let [to (if (= to-addr 0 ) _r (get-receptor _r to-addr))
              user (get-receptor _r (resolve-name _r from-user))
              ]
          (--> (eval (symbol (str "anansi.receptor." (name (:type @to)) "/" signal))) user to params)))

(signal interface authenticate [_r _f {user :user}]
        (rsync _r
               (let [sessions (get-scape _r :session)
                     time (now)
                     iface (:type @(get-receptor _r _f))
                     s (sha (str time "-" iface))
                     user-address (resolve-name _r user)
                     ]
                 (if (nil? user-address) (throw (RuntimeException. (str "authentication failed for user: " user))))
                 (--> key->set _r sessions s {:user user-address :time time :interface iface})
                 s)))

(signal interface new-user [_r _f {user :user}]
        (if (resolve-name _r user)
          (throw (RuntimeException. (str "username '" user "' in use"))))
        (rsync _r
               (let [addr (address-of (receptor :user _r user nil))]
                 (--> key->set _r (get-scape _r :user) user addr)
                 addr))
        )

(signal ceptr ping [_r _f _]
        (str "Hi " _f "! This is the host."))
