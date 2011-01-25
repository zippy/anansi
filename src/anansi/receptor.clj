(ns
  #^{:author "Eric Harris-Braun"
     :doc "Receptors"}
  anansi.receptor)

(defprotocol Receptor
  "Base protocol for all receptors"
  (receive [this signal])
  (get-aspects [this])
  )

;;;;;;;;;;;;   Utility Functions ;;;;;;;;;;;;

(defn parse-address [address] 
  "Utility function to parse a string encoded ceptr address into hash"
  (if (string? address)
    (let [[id aspect] (.split #"\." address)]
      {:id id,:aspect (keyword aspect)})
    address))

(defn parse-signal-addresses [signal]
  "Utility function to parse a string encoded addresses in a signal"
  (reduce #(assoc %1 %2 (parse-address (%2 signal))) [signal :from :to]))

(defn parse-signal [signal]
  "Utility function to parse a string encoded signal"
  (parse-signal-addresses
   (if (string? signal)
     (read-string signal)
     signal)))

(defn
  ^{:doc "Utility function that confirms generic validity of a signal"}
  validate-signal
  ([receptor signal] (validate-signal receptor signal false))
  ([receptor signal do-raise]
    (let [{:keys [to]} (parse-signal signal) 
          {:keys [aspect]} (parse-address to)
          ]
        (if (nil? (aspect (get-aspects receptor)))
          (let [err_str  (str "unknown aspect " aspect)]
            (if do-raise
                (throw (RuntimeException. (str "Invalid signal (error was " err_str ")")))
                (assoc signal :error err_str)))
          signal))))

;; RECEPTORS

(defrecord SimpleReceptor [_]
  Receptor
  (get-aspects [this] #{:simple})
  (receive [this signal] 
     (let [{:keys [from to body error]} (validate-signal this signal true)]
      (str "I got '" body "' from: " (if (string? from) from (str (:id from) "." (name  (:aspect from )))))
      )))

(defrecord MembraneReceptor [receptors]
  Receptor
  (get-aspects [this] #{:create})
  (receive [this signal] 
    (let [parsed-signal (parse-signal signal)
          {:keys [from to body]} parsed-signal]
      (if (= :create (:aspect to))
        ;; add a new receptor into the membranes receptor list if
        ;; signal received on the :create aspect
        (dosync (alter receptors assoc (:name body) (SimpleReceptor. nil))
                "created")
        ;; otherwise assume the signal is sent to one of our contained
        ;; receptors
        (let [name (:id to)
              receptor (@receptors name)]
          (if (nil? receptor)
            (throw (RuntimeException. (str "Receptor '" name "' not found")))
            (receive receptor parsed-signal)
            )
          )
        )
    )))

(defn create-membrane
  "Utility function to create an empty membrane receptor"
  []
  (MembraneReceptor. (ref {})))

(defrecord UserReceptor [user-name attributes]
  Receptor
  (get-aspects [this] #{:get-attributes :set-attributes :receive-object :release-object})
  (receive [this signal] 
    (let [parsed-signal (parse-signal signal)
          {:keys [from to body]} parsed-signal]
      (condp = (:aspect to)
        :get-attributes (let [{:keys [keys]} body] (if (nil? keys) @attributes (select-keys @attributes keys))) 
        :set-attributes (dosync (alter attributes merge body))
        :receive-object "not-implemented"
        :release-object "not-implemented"
        )
)))

(defn create-user
  "Utility function to create a user receptor"
  [user-name]
  (UserReceptor. user-name (ref {})))

