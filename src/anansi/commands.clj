(ns
  #^{:author "Eric Harris-Braun"
     :doc "Commands that can be excecuted at the servers command line"}  
  anansi.commands
  (:use [anansi.user]
        [anansi.receptor :only [receive parse-signal dump-receptor serialize-receptor]]
        [anansi.server-constants])
  (:use [clojure.string :only [join]]))

;; Generic Utilities - FIXME move to utility file

(defn- modify-keys [tform m]
  "takes fn and map and returns new map with fn applied to keys"
  (into {} (map (fn [[k v]] { (tform k) v }) m)))

(defn- get-first [the-map & keys]
  "returns first non-nil value from looking up keys in the-map"  
  (first (filter (complement nil?) (map #(get the-map %) keys))))

;; Command Utilities

(defn- command-name [method]
  "given a clojure method, return anansi command name (from metadata)"
  (str (get-first (meta method) :command-name :name)))

(defn- command-index []
  "return map of anansi command-names to clojure methods"
  (let [fn-map (dissoc (ns-publics 'anansi.commands) 'execute)]
    (modify-keys #(command-name (fn-map %)) fn-map)))

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
   ([& signal]
      (receive *server-receptor* (assoc (parse-signal (join " " signal)) :from {:id *user-name* :aspect "?"})))
     )

(defn help
  "Show available commands and what they do."
  []
  (join "\n" (map (fn [[name f]] (str name ": " (:doc (meta f))))
                  (command-index))))

;; Command handling
(defn execute
  "Execute a command."
  [input]
  (try (let [[command & args] (.split input " +")
             command-function ((command-index) command)
             ]
         (if (nil? command-function)
           (str "Unknown command: '" input  "'. Try help for a list of commands.")
           (apply command-function args )))
       (catch Exception e
         (.printStackTrace e *err*)
         (str "ERROR: " e)
         )))
