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
  [host iface input]
  (try (let [[command arg-part] (.split input " +" 2)
             args (cond (nil? arg-part) nil
                        (= \{ (first arg-part)) [arg-part]
                        true (into [] (.split arg-part " +")))
             command-function ((command-index) command)]
         (if (nil? command-function)
           {:status :error
            :result (str "Unknown command: '" command "'")
            :comment  "Try 'help' for a list of commands."}
           {:status :ok
            :result (apply command-function (conj (seq args) iface host) )}))
       (catch Exception e
         (.printStackTrace e *err*)
         {:status :error
          :result (.getMessage e)})))

(defn authenticate [host iface user]
  (--> interface->authenticate iface host {:user user})
  )

(defn new-user [host iface user]
  (--> interface->new-user iface host {:user user})
  )
