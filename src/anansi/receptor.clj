(ns
  #^{:author "Eric Harris-Braun"
     :doc "Receptor helper functions and receptor definitions"}
  anansi.receptor
  (:use [clojure.string :only [join]]))

(defprotocol Receptor
  "Underlying protocol for all receptors
Methods:
  (receive [this signal])
  (get-aspects [this])  => returns a set of all aspects impelemented by the receptor
"
  (receive [this signal])
  (get-aspects [this])
  )

;;;;;;;;;;;;   Utility Functions ;;;;;;;;;;;;

(defn humanize-address
  "turn an address into a human readable string"
  [{:keys [id aspect], :as address} ]
  (if (string? address) address (str id ":" (clojure.core/name aspect)))
  )

 (defn parse-address [address] 
  "Utility function to parse a string encoded ceptr address into hash"
  (if (string? address)
    (let [[id aspect] (.split #":" address)]
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

(defn throw-bad-aspect
  "Utility function to throw an error when a receptor is sent a signal on no exitent aspect"
  [to]
  (throw (RuntimeException. (str "Invalid aspect for " (humanize-address to)))))

;; RECEPTORS

(defrecord ObjectReceptor [name]
  Receptor
  (get-aspects [this] #{:ping})
  (receive [this signal] 
     (let [{:keys [from to body error]} (validate-signal this signal true)]
      (str "I got '" body "' from " (humanize-address from))
      )))

(defn make-object
  "Utility function to create an empty object receptor"
  [name]
  (ObjectReceptor. name))

(declare conjure-receptor)

(defn resolve-address
  "Utility function to resolve an address to :self or to a contained receptor in the scape"
  [{:keys [receptors self], :as scape} {:keys [id aspect], :as address}]
  (let  [[head & rest] (.split #"\." id)]
    [(if (= self head) :self (@receptors head))
     (if (nil? rest) address (assoc address :id (join "." rest)))])
  )

(defn membrane-receive
  "Default membrane receive function.  Handles :conjure aspect and routing signals to contained receptors"
  [this signal scape] 
  (let [parsed-signal (parse-signal signal)
        {:keys [from to body]} parsed-signal
        [destination-receptor resolved-address] (resolve-address @scape to)]
    (if (= destination-receptor :self) 
      (condp = (:aspect to)
          ;; add a new receptor into the membranes receptor list
          :conjure (dosync (let [{:keys [name]} body] 
                             (alter (@scape :receptors) assoc (:name body) (conjure-receptor body)))
                           "created")
        (throw-bad-aspect to)
        )

      (if (nil? destination-receptor)
          (throw (RuntimeException. (str "No route to '" (humanize-address to) "'")))
          (receive destination-receptor (assoc parsed-signal :to resolved-address))))))

(defrecord MembraneReceptor [scape]
  Receptor
  (get-aspects [this] #{:conjure})
  (receive [this signal] (membrane-receive this signal scape))
  )

(defn make-scape
  "Utility function to create an empty scape for a new membrane"
  [self-name]
  (ref {:receptors (ref {}), :self self-name}))

(defn make-membrane
  "Membrane factory

membranes receive the following signals:
     aspect: conjure  -- create a new receptor inside the membrane
       body: {:name <receptor-name>, :type <receptor-type>, ...<other keys as defined by the recptor type>}
    returns: \"created\" if successful
"
  [name]
  (MembraneReceptor. (make-scape name)))

(defrecord ServerReceptor [scape]
  Receptor
  (get-aspects [this] #{:conjure})
  (receive [this signal] (membrane-receive this signal scape))
  )

(defn make-server
  "Sever factory

servers are membranes that also receive the following signals:
     aspect: 
       body: {:name <receptor-name>, :type <receptor-type>, ...<other keys as defined by the recptor type>}
    returns: \"\" if successful
"
  [name]
  (ServerReceptor. (make-scape name)))

(defrecord PersonReceptor [name attributes]
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
        (throw-bad-aspect to)
        )
)))

(defn make-person
  "Utility function to create a person receptor"
  [name]
  (PersonReceptor. name (ref {})))

(defn conjure-receptor
  "Create a new receptor based on the parameters specified in the body of the signal"
  [body]
  (let [{:keys [type name]} body]
    (condp = type
        "Object"
      (make-object (:name body))
      "Membrane"
      (make-membrane (:name body))
      (throw (RuntimeException. (str "Unknown receptor type: '" type "'")))
      )))

