(ns
  #^{:author "Eric Harris-Braun"
     :doc "Commands that can be excecuted at the server's command line"}  
  anansi.commands
  (:use [anansi.user]
        [anansi.util :only [modify-keys modify-vals]]
        [anansi.receptor
         :only [receive parse-signal dump-receptor serialize-receptor]]
        [anansi.server-constants]
        [anansi.ceptr])
  (:use [clojure.string :only [join]]
        [clojure.contrib.strint]
        [clojure.contrib.json :only [read-json]]))

;; Command Utilities

(defn- command-name [method]
  (str (some #((meta method) %) [:command-name :name])))

(defn- command-index  []
  (let [fn-map (dissoc (ns-publics 'anansi.commands) 'execute)]
    (modify-keys #(command-name (fn-map %)) fn-map)))

(defn execute
  "Execute a command."
  [input]
  (try (let [[command arg-part] (.split input " +" 2)
             args (cond (nil? arg-part) nil
                        (= \{ (first arg-part)) [arg-part]
                        true (into [] (.split arg-part " +")))
             command-function ((command-index) command)]
         (if (nil? command-function)
           {:status :error
            :result (str "Unknown command: '" input "'")
            :comment  "Try 'help' for a list of commands."}
           {:status :ok
            :result (apply command-function args )}))
       (catch Exception e
         (.printStackTrace e *err*)
         {:status :error
          :result (str "exception raised: " e)})))

;; Commands

(defn users
  "Get a list of logged in users"
  []
  (vec (keys @user-streams)))
;  (modify-vals state @user-streams)

(defn exit
  "Terminate connection with the server"
  []
  (let [bye_str (str "Goodbye " *user-name* "!")]
    (dosync
     (set! *done* true)
     ;(commute user-streams assoc *user-name* nil)
     )
    (spit *server-state-file-name* (serialize-receptors *receptors*))
    bye_str))

(defn dump
  "Dump current tree of receptors"
  []
  (str (:receptors- (dump-receptor *server-receptor*))))

(defn
  #^{ :doc "Send a signal to a receptor.", :command-name "send"}
  send-signal
  [j]
  (receive *server-receptor*
           (assoc (parse-signal (read-json j))
             :from {:id *user-name* :aspect "?"})))

(defn help
  "Show available commands and what they do."
  ([]
     (join "\n" (map help (keys (command-index)))))
  ([command]
     (if (contains? (command-index) command)
       (str command ": " (:doc (meta ((command-index) command))))
       (<< "No such command ~{command}"))))

(defn ss
  "Send a signal (new version)"
  [j]
  (let [host (get-host)
        {to-addr :to signal :signal params :params} (read-json j)
        to (if (= to-addr 0 ) host (get-receptor host to-addr))]
       (--> (eval (symbol (str "anansi.receptor." (name (:type @to)) "/" signal))) (@user-streams *user-name*) to params)
       ))

(defn- parse-signal-keyword
  [k]
  (let [[_ r a s] (re-find #"([^.]+)\.([^.]+)\.([^.]+)$" (name k))] [(keyword r) (str a "->" s)])
  )

(defn rl
  "Request a list of all receptor specification on the server"
  []
  
  (reduce (fn [m [k v]] (let [[r s] (parse-signal-keyword k)
                             params (into [] (rest (rest v)))]
                         (if (r m)
                           (update-in m [r] (fn [rm] (assoc rm s params)))
                           (assoc m r {s params}))))
          {} @*signals*)
    
  )

(defn gs
  "Get state"
  [j]
  (let [host (get-host)
        {to-addr :addr full? :full} (read-json j)
        to (if (= to-addr 0 ) host (get-receptor host to-addr))]
    (state to full?)))

