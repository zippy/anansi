(ns
  #^{:author "Eric Harris-Braun"
     :doc "Host receptor"}
  anansi.receptor.host
  (:use [anansi.ceptr]
        [anansi.receptor.commons-room]
        [anansi.receptor.scape]))

(defmethod manifest :host [_r]
           {:name-scape (receptor scape _r)})

;; TODO make this an generalized receptor host
(comment defmacro make-receptor [n p & a] `(receptor ~(symbol (str (name n))) ~p ~@a))
(comment defn do-make-receptor [n p & a] (receptor '(symbol (str (name n))) p a))
(signal self host [_r receptor-name & args]
        (dosync
         (let [names (contents _r :name-scape)
               r (receptor commons-room _r) ;;(make-receptor type _r args)
               addr (address-of r)]
           (key->set names receptor-name addr)
           addr)))
