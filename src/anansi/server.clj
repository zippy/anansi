(ns
  #^{:skip-wiki true}    
   anansi.server
   (:use [anansi.user]
         [anansi.commands :only [execute]]
         [anansi.receptor.user]
         [anansi.receptor.host]
         [anansi.ceptr]
         [anansi.server-constants])
   (:use [clojure.java.io :only [reader writer]]
      [clojure.contrib.server-socket :only [create-server]])
)

(defn- cleanup []
  "Clean user list."
  (dosync
   (let [user (@user-streams *user-name*)]
     (--> self->disconnect *host* user)
     (commute user-streams dissoc *user-name*)))
  (println (str "Cleaning up " *user-name*)))

(defn- get-unique-user-name [name]
  (if (@user-streams name)
    (do (print "That name is in use; try again: ")
        (flush)
        (recur (read-line)))
    name))

(defn anansi-handle-client [in out]
  (binding [*in* (reader in)
            *out* (writer out)]

    ;; We have to nest this in another binding call instead of using
    ;; the one above so *in* and *out* will be bound to the socket
    (print "\nWelcome to the Anansi sever.\n\nNote: All signals directed to this sever should be addressed to \"server\"\n\nEnter your user name: ") (flush)
    (binding [*user-name* nil]
      (dosync
       (set! *user-name* (get-unique-user-name (read-line)))
       (let [users (contents *host* :user-scape)
             user-address (s-> self->host-user *host* *user-name*) ;; creates or returns existing user receptor address
             user (get-receptor *host* user-address)]
         (--> self->connect *host* user *out*)
         (commute user-streams assoc *user-name* user)))

      (print prompt) (flush)

      (try (loop [input (read-line)]
             (when input
               (println (execute input))
               (print prompt) (flush)
               (if (user-streams *user-name*) (recur (read-line)) )
               ))
           (finally (cleanup))))))

(defn launch-server [port]
          (defonce server (create-server (Integer. port) anansi-handle-client))
          (println "Launching Anansi server on port" port)
          )
