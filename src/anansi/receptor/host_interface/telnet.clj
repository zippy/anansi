(ns
  #^{:author "Eric Harris-Braun"
     :doc "Telnet host interface receptor"}
  anansi.receptor.host-interface.telnet
  (:use [anansi.ceptr]
        [anansi.receptor.user]
        [anansi.receptor.scape]
        [anansi.receptor.host]
        [anansi.receptor.host-interface.commands :only [execute]])
  (:use [clojure.java.io :only [reader writer]]
        [clojure.contrib.server-socket :only [create-server]]))

(defmethod manifest :telnet-host-interface [_r {}]
           {})
(defmethod state :telnet-host-interface [_r full?]
           (state-convert _r full?))
(defmethod restore :telnet-host-interface [state parent]
           (let [r (do-restore state parent)]
             r))

(comment dosync
       (set! *user-name* (get-unique-user-name (read-line)))
       (let [host (get-host)
             x  (if (nil? host) (throw (RuntimeException. (str host "R: " (keys @*receptors*)))))
             users (get-scape host :user)
             user-address (s-> self->host-user host *user-name*) ;; creates or returns existing user receptor address
             user (get-receptor host user-address)]
         (--> key->set host users *user-name* user-address )
         (--> self->connect host user *out*)
         (commute user-streams assoc *user-name* user)
         (pprint-json {:status :ok
                       :result {:user-address user-address
                                :host-address 0}})
         (print "\n")))
(def *done*)
(def *user*)
(def *session*)
(def *prompt* (ref "\n> "))

(defn parse-and-execute [_r host input]
  (let [[command arg-part] (.split input " +" 2)]
    (condp = command
        "new-user" (execute host _r command {:user arg-part})
        {:status :error
            :result (str "Unknown command: '" command "'")}))
  )

(defn make-handle-connection [host _r]
  (fn [in out]
    (let [from-ip (comment .getHostAddress (.getInetAddress s))]
      (binding [*in* (reader in)
                *out* (writer out)
                ]

        ;; We have to nest this in another binding call instead of using
        ;; the one above so *in* and *out* will be bound to the socket
        (print "\nWelcome to the Anansi sever.\n\nEnter your user name: ") (flush)
        (try
          (binding [*done* false
                    *user* nil
                    *session* nil]
            (loop [user-name (read-line)]
              (when user-name
                (let [{status :status result :result} (execute host _r "authenticate" {:user user-name})]
                  (if (= :ok status)
                    (do
                      (println (str "OK " result))
                      (flush)
                      (set! *user* user-name)
                      (set! *session* result))
                    (do (print (str "ERROR " result "\nEnter your user name: "))
                        (flush)
                        (recur (read-line))))))
              )
            (if (not (nil? @*prompt*)) (print @*prompt*))
                 (flush)
                 (loop [input (read-line)]
                   (when input
                     (let [{status :status result :result} (parse-and-execute _r host input)]
                       (println (str (if (= status :ok) "OK" "ERROR") " " result) ))
                     (if (not (nil? @*prompt*)) (print @*prompt*))
                     (flush)
                     (if (not *done*) (recur (read-line)) )
                     ))
                 )          
          (finally nil (comment cleanup))
  ))))

)
