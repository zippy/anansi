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
  (try (let [command-function ((command-index) command)]
         (if (nil? command-function)
           {:status :error
            :result (str "Unknown command: '" command "'")
            :comment  "Try 'help' for a list of commands."}
           {:status :ok
            :result (command-function host iface params)}))
       (catch Exception e
         (.printStackTrace e *err*)
         {:status :error
          :result (.getMessage e)})))

(defn authenticate [host iface params]
  (--> interface->authenticate iface host params)
  )

(defn new-user [host iface params]
  (--> interface->new-user iface host params)
  )

(defn send-signal [host iface p]
  (let [{prefix :prefix aspect :aspect signal-name :signal params :params session :session to-addr :to} p
        {user-addr :user} (--> key->resolve iface (get-scape host :session) session)
        to (if (= to-addr 0) host to-addr)
        user (get-receptor host user-addr)
        signal-function (get-signal-function (str "anansi." prefix) aspect signal-name)]
    (if (nil? signal-function) (throw (RuntimeException. (str "Unknown signal: " prefix "." aspect "->" signal-name) )))
    (--> signal-function user to params)
  ))
