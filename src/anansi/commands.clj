(ns
  #^{:author "Eric Harris-Braun"
     :doc "Commands that can be excecuted at the servers command line"}  
  anansi.commands
  (:use [anansi.user]
        [anansi.util :only [get-first modify-keys]]
        [anansi.receptor
         :only [receive parse-signal dump-receptor serialize-receptor]]
        [anansi.server-constants])
  (:use [clojure.string :only [join]]
        [clojure.contrib.strint]))

;; Command Utilities

(defn- command-name [method]
  (str (get-first (meta method) :command-name :name)))

(defn- command-index  []
  (let [fn-map (dissoc (ns-publics 'anansi.commands) 'execute)]
    (modify-keys #(command-name (fn-map %)) fn-map)))

(defn execute
  "Execute a command."
  [input]
  (try (let [[command & args] (.split input " +")
             command-function ((command-index) command)]
         (if (nil? command-function)
           (str "Unknown command: '" input
                "'. Try help for a list of commands.")
           (apply command-function args )))
       (catch Exception e
         (.printStackTrace e *err*)
         (str "ERROR: " e))))

;; Commands

(defn users
  "Get a list of logged in users"
  []
  (str (vec (keys @user-streams))))

(defn exit
  "Terminate connection with the server"
  []
  (let [bye_str (str "Goodbye " *user-name* "!")]
    (dosync
     (commute user-streams assoc *user-name* nil))
    (spit *server-state-file-name* (serialize-receptor *server-receptor*))
    bye_str))

(defn dump
  "Dump current tree of receptors"
  []
  (str (:receptors- (dump-receptor *server-receptor*))))

(defn
  #^{ :doc "Send a signal to a receptor.", :command-name "send"}
  send-signal
  [& signal]
  (receive *server-receptor*
           (assoc (parse-signal (join " " signal))
             :from {:id *user-name* :aspect "?"})))

(defn help
  "Show available commands and what they do."
  ([]
     (join "\n" (map help (keys (command-index)))))
  ([command]
     (if (contains? (command-index) command)
       (str command ": " (:doc (meta ((command-index) command))))
       (<< "No such command ~{command}"))))


