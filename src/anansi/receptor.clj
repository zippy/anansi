(ns
  #^{:author "Eric Harris-Braun"
     :doc "Receptor helper functions and receptor definitions"}
  anansi.receptor
  (:require [clojure.string :as str])
  )

(defprotocol Ceptr
  "Underlying protocol for all receptors
Methods:
  (receive [this signal])
  (get-aspects [this])  => returns a set of all aspects impelemented by the receptor
"
  (receive [this signal])
  (get-aspects [this])
  )

;;;;;;;;;;;;   Utility Functions ;;;;;;;;;;;;
(defn dump-receptor
  "dump contents of receptor into pretty-print-ready datastructure"
  [receptor]
  (let [scape @(:scape receptor)
        name (:self scape)
        receptors @(:receptors scape)]
    { :name name
      :contents (if (empty? receptors) #{}
                    (apply hash-set (vec (map (fn [[key value]] (dump-receptor value)) receptors))))}))

(defn humanize-address
  "Utility function turn an address into a human readable string"
  [{:keys [id aspect], :as address} ]
  (if (string? address) address (str id ":" (clojure.core/name aspect)))
  )

(defn sanitize-for-address
  "Utility function to create and address ready string from a string"
  [name]
  (str/replace (str/lower-case name) #"\W" "_")
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

(defn resolve-address
  "Utility function to resolve an address to :self or to a contained receptor in the scape"
  [{:keys [receptors self], :as scape} {:keys [id aspect], :as address}]
  (let  [[head & rest] (.split #"\." id)]
    [(if (= self head) :self (@receptors head))
     (if (nil? rest) address (assoc address :id (str/join "." rest)))])
  )

(defn aspect-receive-dispatch
  "Dispatches to aspect-receive calls based on the class of the recptor or a force"
  ([this signal scape klass]
      (if klass klass (class this)))
  ([this signal scape] (aspect-receive-dispatch this signal scape nil))
  )

(declare make-receptor-from-signal)

(defn do-conjure
  "conjure a recptor into a scape"
  [scape body]
  (dosync (let [{:keys [name]} body] 
                           (alter (@scape :receptors) assoc (:name body) (make-receptor-from-signal body)))
                         "created"))

(defn do-ping
  "return a ping string"
  [from body]
  (str "I got '" body "' from " (humanize-address from))
  )

(defmulti receptor-aspects aspect-receive-dispatch)

;; The default dispatch for receptor-aspects is the "base" receptor.
(defmethod receptor-aspects :default
  [this signal scape & _]
  (let [{:keys [from to body]} signal]
    (condp = (:aspect to)
        
        ;; respond to a ping request
        :ping (do-ping from body) 

        ;; add a sub receptor into the receptors scape
        :conjure (do-conjure scape body)
        
        ;; otherwise throw an error
        (throw-bad-aspect to))))

(defn receptor-receive
  "Receive a signal, and either route it to a sub-receptors, or dispatch to the aspect handler."
  [this signal scape] 
  (let [parsed-signal (parse-signal signal)
        {:keys [from to body]} parsed-signal
        [destination-receptor resolved-address] (resolve-address @scape to)]
    (if (= destination-receptor :self)
      (receptor-aspects this parsed-signal scape)
      (if (nil? destination-receptor)
          (throw (RuntimeException. (str "No route to '" (humanize-address to) "'")))
          (receive destination-receptor (assoc parsed-signal :to resolved-address))))))

(defrecord Receptor [scape]
  Ceptr
  (get-aspects [this] #{:conjure :ping})
  (receive [this signal] (receptor-receive this signal scape))
  )

(defn make-scape
  "Utility function to create an empty scape for a new receptor"
  ( [self-name attributes]
      (let [ scape {:receptors (ref {}), :self self-name}]
        (ref (if attributes (merge scape attributes) scape))))
  ( [self-name] (make-scape self-name nil)))

(defn make-receptor
  "Receptor factory

vanilla receptors receive the following signals:
     aspect: conjure  -- create a new receptor inside this receptor
       body: {:name <receptor-name>, :type <receptor-type>, ...<other keys as defined by the recptor type>}
    returns: \"created\" if successful

     aspect: ping  -- request a ping response
       body: X
    returns: \"I got '<body>' from <from>\"
"
  [name]
  (Receptor. (make-scape name)))


(defrecord ObjectReceptor [scape]
  Ceptr
  (get-aspects [this] #{:ping :conjure})
  (receive [this signal] 
     (let [{:keys [from to body error]} (validate-signal this signal true)]
      (do-ping from body)
      )))

(defn make-object
  "Utility function to create an empty object receptor"
  [name]
  (ObjectReceptor. (make-scape name)))

(defrecord ServerReceptor [scape]
  Ceptr
  (get-aspects [this] #{:ping :conjure})
  (receive [this signal] (receptor-receive this signal scape))
  )

(defn make-server
  "Sever factory

servers receive the following signals:
     aspect: 
       body: {:name <receptor-name>, :type <receptor-type>, ...<other keys as defined by the recptor type>}
    returns: \"\" if successful
"
  [name]
  (ServerReceptor. (make-scape name)))

(defrecord RoomReceptor [scape]
  Ceptr
  (get-aspects [this] #{:ping :conjure :describe :enter :leave :scape :pass-object})
  (receive [this signal] (receptor-receive this signal scape)))

(defn find-receptor
  "Utility function to lookup up a receptor in a scape by address"
  [scape address]
  (@(@scape :receptors) address)
  )

(defn remove-receptor
  "Utility function to remove a receptor from a scape by address"
  [scape address]
  (alter (@scape :receptors) dissoc address)
  )

(declare make-person)
(defmethod receptor-aspects anansi.receptor.RoomReceptor
  [this signal scape]
  (let [{:keys [to body]} signal
        people-ref (@scape :people)
        ]
    (condp = (:aspect to)
        :describe (str (vec (map :name (vals @people-ref))))
        :enter (let [{:keys [person]} body
                     {:keys [name]} person
                     person-address (sanitize-for-address name)
                     person-receptor (find-receptor scape person-address)
                     ]
                 (if person-receptor
                   (str name " is already in the room")
                   (dosync (alter people-ref assoc person-address {:name name})
                           (do-conjure scape {:name person-address, :attributes {:name name} :type "Person"})
                           (str "entered as " person-address)) 
                   )
                 )
        :leave (let [{:keys [person-address]} body
                     person-receptor (find-receptor scape person-address)]
                 (if (nil? person-receptor)
                   (str person-address " is not in the room")
                   (dosync (remove-receptor scape person-address)
                           (alter people-ref dissoc person-address)
                           (str person-address " left"))))
        :pass-object nil
        (receptor-aspects this signal scape :default))))

(defn make-room
  "Room factory

rooms are receptors that also receive the following signals:
     aspect: describe
       body: nil
    returns: <vector of people in the room>

     aspect: enter
       body: {:person {:name <some name> :email \"email@example.com\" <other-person-key-value-pairs>}}
    returns: \"entered\" if successful

     aspect: leave
       body: {:person-address <some-addr>}
    returns: \"left\" if successful

     aspect: scape
       body: {<scaping-attributes>}
    returns: \"ok\" if successful

     aspect: pass-object
       body: {:object \"<object-address>\" :to {:person \"<person-address>\"}|:right|:left}
    returns: \"ok\" if successful
"
  [name]
  (RoomReceptor. (make-scape name {:objects (ref {}), :people (ref {})})))

(defrecord PersonReceptor [scape attributes]
  Ceptr
  (get-aspects [this] #{:ping :conjure :get-attributes :set-attributes :receive-object :release-object})
  (receive [this signal] 
    (let [parsed-signal (parse-signal signal)
          {:keys [from to body]} parsed-signal]
      (condp = (:aspect to)
        :get-attributes (let [{:keys [keys]} body] (if (nil? keys) @attributes (select-keys @attributes keys))) 
        :set-attributes (dosync (alter attributes merge body))
        :receive-object "not-implemented"
        :release-object "not-implemented"
        (receptor-aspects this signal scape :default)
        )
)))

(defn make-person
  "Utility function to create a person receptor"
  ([name] (make-person name {}))
  ([name attributes]
     (PersonReceptor. (make-scape name) (ref attributes))))

(defn make-receptor-from-signal
  "Create a new receptor based on the parameters specified in the body of the signal"
  [body]
  (let [{:keys [type name]} body]
    (condp = type
        "Object"
      (make-object (:name body))
      "Receptor"
      (make-receptor (:name body))
      "Person"
      (make-person (:name body) (:attributes body))
      (throw (RuntimeException. (str "Unknown receptor type: '" type "'")))
      )))

