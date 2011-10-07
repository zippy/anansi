(ns
  #^{:author "Eric Harris-Braun"
     :doc "Host receptor"}
  anansi.receptor.host
  (:use [anansi.ceptr]
        [anansi.receptor.user]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.libs.sha :only [sha]])
  (:use [clj-time.core :only [now]]))

(def host-def (receptor-def "host" (scapes
                                    {:name :room :relationship {:key :name :address :address}}
                                    {:name :user :relationship {:key :name :address :address}}
                                    {:name :stream :relationship {:key :name :address :address}}
                                    {:name :creator :relationship {:key :address :address :creator-address}}
                                    {:name :session :relationship {:key :sha :address :user-addr-time-interface-map}})))

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
                     creators (get-scape _r :creator)
                     r (receptor :commons-room _r matrice-address password data) ;;(make-receptor type _r args)
                     addr (address-of r)]
                 (--> key->set _r names receptor-name addr)
                 (--> key->set _r creators addr _f)
                 addr)))
(signal self host-streamscape [_r _f {receptor-name :name password :password matrice-address :matrice-address data :data}]
        (rsync _r
               (let [names (get-scape _r :stream)
                     creators (get-scape _r :creator)
                     r (make-receptor streamscapes-def _r {:matrice-addr matrice-address :attributes {:_password  password :data data}}) ;;(make-receptor type _r args)
                     addr (address-of r)]
                 (--> key->set _r names receptor-name addr)
                 (--> key->set _r creators addr _f)
                 addr)))
(signal self host-user [_r _f receptor-name]
        (rsync _r
               (let [names (get-scape _r :user)
                     creators (get-scape _r :creator)
                     existing-addr (--> key->resolve _r names receptor-name)
                     addr (if existing-addr existing-addr (address-of (make-receptor user-def _r receptor-name))) ;; (make-receptor type _r args)
                     ]
                 (--> key->set _r names receptor-name addr)
                 (--> key->set _r creators addr _f)
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
                     iface (rdef (get-receptor _r _f) :fingerprint)
                     s (sha (str time "-" iface))
                     user-address (resolve-name _r user)
                     ]
                 (if (nil? user-address) (throw (RuntimeException. (str "authentication failed for user: " user))))
                 (--> key->set _r sessions s {:user user-address :time (str time) :interface iface})
                 {:session s})))

(signal command new-user [_r _f {user :user}]
        (if (resolve-name _r user)
          (throw (RuntimeException. (str "username '" user "' in use"))))
        (rsync _r
               (let [addr (address-of (make-receptor user-def _r user))]
                 (--> key->set _r (get-scape _r :user) user addr)
                 addr))
        )

(signal command get-state [_r _f {receptor-address :receptor query :query}]
        (let [receptor (if (= 0 receptor-address) _r (get-receptor _r receptor-address))]
          (if (nil? receptor)
            (throw (RuntimeException. (str "unknown receptor: " receptor-address))))
          (receptor-state receptor (if (nil? query) false query))))

(signal ceptr ping [_r _f _]
        (str "Hi " _f "! This is the host."))
