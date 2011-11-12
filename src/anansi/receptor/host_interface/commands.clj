(ns
  #^{:author "Eric Harris-Braun"
     :doc "Commands that can be excecuted by a host interface"}  
  anansi.receptor.host-interface.commands
  (:use [anansi.ceptr]
        [anansi.util :only [modify-keys]]
        [anansi.receptor.user]
        [anansi.receptor.scape]
        [anansi.receptor.host]))

(defn- command-name [method]
  (str (some #((meta method) %) [:command-name :name])))

(defn- command-index  []
  (let [fn-map (dissoc (ns-publics 'anansi.receptor.host-interface.commands) 'execute)]
    (modify-keys #(command-name (fn-map %)) fn-map)))

(defn execute
  "Execute a command."
  [host iface command params]
  (time (try (let [start-time (now)
                   command-function ((command-index) command)
                   result (if (nil? command-function)
                            {:status :error
                             :result (str "Unknown command: '" command "'")
                             :comment  "Try 'help' for a list of commands."}
                            (let [r (command-function host iface params)]
                              (if (log-level-verbose) (prn "RESULT OF COMMAND WAS:" r))
                              {:status :ok
                               :result r}))]
               (println (str "Executing " command))
               result
               )
             (catch Exception e
               (.printStackTrace e *err*)
               {:status :error
                :result (.getMessage e)}))))

(defn authenticate [host iface params]
  (--> command->authenticate iface host params)
  )

(defn new-user [host iface params]
  (--> command->new-user iface host params)
  )

(defn send-signal [host iface params]
  (--> command->send-signal iface host params)
)

(defn get-state [host iface params]
  (--> command->get-state iface host params)
)
