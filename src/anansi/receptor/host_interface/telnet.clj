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
        [clojure.contrib.server-socket :only [create-server close-server]]))

(def telnet-def (receptor-def "telnet" (manifest [_r {}] {:server nil})))

(def *done*)
(def *user*)
(def *session*)
(def *prompt* (ref "\n> "))

(defn parse-and-execute [_r host input]
  (let [[command arg-part] (.split input " +" 2)]
    (condp = command
        "new-user" (execute host _r command {:user arg-part})
        "send" (let [[_ to prefix aspect signal] (re-find #"^([0-9]+) (.*\..*)\.(.*)->(.*)$" arg-part)]
                 (execute host _r "send-signal" {:to to :prefix prefix :aspect aspect :signal signal :session *session*}))
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
        (print "\nWelcome to the Anansi server.\n\nEnter your user name: ") (flush)
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
                      (set! *session* (:session result)))
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
  )))))

(signal interface start [_r _f {port :port}]
        (if (contents _r :server)
          (throw (RuntimeException. "Server already started."))
          (rsync _r
                 (let [host (get-receptor (parent-of _r) _f)]
                   (set-content _r :server (create-server (Integer. port) (make-handle-connection host _r)))))))

(signal interface stop [_r _f]
        (let [server (contents _r :server)]
          (if (not server)
            (throw (RuntimeException. "Server not started."))
            (rsync _r
                   (close-server server)
                   (set-content _r :server nil)))))
