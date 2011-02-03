(ns
  #^{:author "Eric Harris-Braun"
     :doc "Commands that can be excecuted at the servers command line"}  
  anansi.commands
  (:use [anansi.user]
        [anansi.receptor :only [receive parse-signal dump-receptor make-server]])
  (:use [clojure.string :only [join]]))

(def *server-receptor* (make-server "server"))

(defn exit
  "Terminate connection with the server"
  []
  (let [bye_str (str "Goodbye " *user-name* "!")]
    (set! *user-name* :exit)
    bye_str))

(defn dump
  "Dump current tree of receptors"
  []
  (str (:receptors- (dump-receptor *server-receptor*))))

(defn help
  "Show available commands and what they do."
  []
  (join "\n" (map #(let [m (meta (val %))
                              doc (:doc m)
                              name (if (nil? (:command-name m)) (:name m) (:command-name m) ) 
                              ]
                          (str name ": " doc)
                          )
                      (dissoc (ns-publics 'anansi.commands)
                              'execute 'commands '*server-receptor*))))

(defn
  #^{ :doc "Send a signal to a receptor.", :command-name "send"}
  send-signal
   ([& signal]
      (receive *server-receptor* (assoc (parse-signal (join " " signal)) :from {:id *user-name* :aspect "?"})))
     )

;; Command data

(def commands
  {"help" help
   "exit" exit
   "dump" dump
   "send" send-signal})

;; Command handling

(defn execute
  "Execute a command."
  [input]
  (try (let [[command & args] (.split input " +")
             command-function (commands command)
             ]
         (if (nil? command-function)
           (str "Unknown command: '" input  "'. Try help for a list of commands.")
           (apply command-function args )))
       (catch Exception e
         (.printStackTrace e *err*)
         (str "ERROR: " e)
         )))
