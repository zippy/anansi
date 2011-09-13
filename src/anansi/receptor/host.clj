(ns
  #^{:author "Eric Harris-Braun"
     :doc "Host receptor"}
  anansi.receptor.host
  (:use [anansi.ceptr]
        [anansi.receptor.commons-room]
        [anansi.receptor.user]
        [anansi.receptor.scape]
        [anansi.libs.sha :only [sha]])
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

(signal command send-signal [_r _f p]
        (rsync _r 
               (let [{prefix :prefix aspect :aspect signal-name :signal params :params session :session to-addr :to} p
                     {user-addr :user} (--> key->resolve _r (get-scape _r :session) session)
                     _ (if (nil? user-addr) (throw (RuntimeException. (str "Unknown session: " session))))
                     to (if (= to-addr 0) _r to-addr)
                     user (get-receptor _r user-addr)
                     signal-function (get-signal-function (str "anansi." prefix) aspect signal-name)]
                 (if (nil? signal-function) (throw (RuntimeException. (str "Unknown signal: " prefix "." aspect "->" signal-name))))
                 (--> signal-function user to params))))

(signal command authenticate [_r _f {user :user}]
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

(signal command new-user [_r _f {user :user}]
        (if (resolve-name _r user)
          (throw (RuntimeException. (str "username '" user "' in use"))))
        (rsync _r
               (let [addr (address-of (receptor :user _r user nil))]
                 (--> key->set _r (get-scape _r :user) user addr)
                 addr))
        )

(signal ceptr ping [_r _f _]
        (str "Hi " _f "! This is the host."))
