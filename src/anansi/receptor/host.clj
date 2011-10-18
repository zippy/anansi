(ns
  #^{:author "Eric Harris-Braun"
     :doc "Host receptor"}
  anansi.receptor.host
  (:use [anansi.ceptr]
        [anansi.receptor.user]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.groove :only [groove-def]]
        [anansi.libs.sha :only [sha]])
  (:use [clj-time.core :only [now]]))

(def host-def (receptor-def
               "host"
               (scapes
                {:name :room :relationship {:key "room-name" :address "room-address"}}
                {:name :user :relationship {:key "user-name" :address "user-address"}}
                {:name :groove :relationship {:key "groove-name" :address "groove-address"}}
                {:name :stream :relationship {:key "streamscapes-name" :address "streamscapes-address"}}
                {:name :creator :relationship {:key "address" :address "creator-user-address"}}
                {:name :session :relationship {:key "sha" :address "user_address_time_interface_map"}})
               (animate [_r reanimate]
                        (if (not reanimate)
                          (let [grooves (get-scape _r :groove)]
                            (if (= 0 (scape-size grooves))
                              (do 
                                (let [groove (make-receptor groove-def _r {:attributes {:grammars {:streamscapes {:subject "text/plain" :body "text/html"}
                                                                                                   :email {:subject "text/plain" :body "text/html"}}}})]
                                  (--> key->set _r grooves :subject-body-message (address-of groove)))
                                (let [groove (make-receptor groove-def _r {:attributes {:grammars {:streamscapes {:message "text/plain"}
                                                                                                   :twitter {:text "text/plain"}
                                                                                                   :irc {:message "text/plain"}}}})]
                                  (--> key->set _r grooves :simple-message (address-of groove)))
                                (let [groove
                                      (make-receptor
                                       groove-def _r
                                       {:attributes {:grammars
                                                     {:streamscapes {:promised-good "text/plain"
                                                                     :expiration "text/plain"}
                                                      :email {:subject {"text" ["Punkmoney Promise"]}
                                                              :body {"text"
                                                                     ["I promise to pay (.*), on demand, ([^.]+)\\. Expires in (.*)\\."
                                                                      {:payee 1
                                                                       :promised-good 2
                                                                       :expiration 3}
                                                                      ]}}
                                                      :twitter {:text {"text"
                                                                       ["^(@[^\\W]+) I promise to pay, on demand, ([^.]+)\\. Expires in (.*)\\. #punkmoney"
                                                                        {:payee 1
                                                                         :promised-good 2
                                                                         :expiration 3}
                                                                        ]}}}}})
                                      ]
                                  (--> key->set _r grooves :punkmoney (address-of groove))))
                              ))))
               ))

(defn resolve-name [_r user]
  "resolve a username to it's receptor address"
  (let [ names (get-scape _r :user)]
    (--> key->resolve _r names user)))

;; TODO make this an generalized receptor host
;; (defmacro make-receptor [n p & a] `(receptor :~(symbol (str (name n))) ~p ~@a))
;; (defn do-make-receptor [n p & a] (receptor :'(symbol (str (name n))) p a))
;(signal self host-room [_r _f {receptor-name :name password :password matrice-address :matrice-address data :data}]
;        (rsync _r
;               (let [names (get-scape _r :room)
;                     creators (get-scape _r :creator)
;                     r (receptor :commons-room _r matrice-address password data) ;;(make-receptor type _r args)
;                     addr (address-of r)]
;                 (--> key->set _r names receptor-name addr)
;                 (--> key->set _r creators addr _f)
;                 addr)))

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
(signal self host-groove [_r _f {receptor-name :name grammars :grammars}]
        (rsync _r
               (let [names (get-scape _r :groove)
                     qualified-name (keyword (str _f "." receptor-name))
                     existing-addr (--> key->resolve _r names qualified-name)
                     x (if existing-addr (throw (RuntimeException. (str "A groove already exists with the name: " qualified-name))))
                     creators (get-scape _r :creator)
                     r (make-receptor groove-def _r {:attributes {:grammars grammars}})
                     addr (address-of r)]
                 (--> key->set _r names qualified-name addr)
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
                 {:session s :creator (--> address->resolve _r (get-scape _r :creator) user-address)})))

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
