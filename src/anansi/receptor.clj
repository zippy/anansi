(ns
  #^{:author "Eric Harris-Braun"
     :doc "Receptor helper functions and receptor definitions"}
  anansi.receptor
  (:use [anansi.user])
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
  (let [contents @(:contents receptor)
        name (:self contents)
        receptors @(:receptors contents)
        attributes (dissoc contents :self :receptors)
        dump-obj {:name- name
                  :type- (let [[_ type]  (re-find #"([a-zA-Z]*)Receptor$" (str (class receptor)))]
                          (if (= "" type) "Receptor" type))
                  :receptors- (if (empty? receptors) #{}
                                 (apply hash-set (vec (map (fn [[key value]] (dump-receptor value)) receptors))))}]
    ;; assume that all other attributes are refs for mutability, so
    ;; copy their values into the dump object
    (into dump-obj (map (fn [[key val]] [key @val]) attributes))  
    ))

(declare receptor-factory)

(defn add-receptor-to-contents
  "adds a sub receptor into the contents ref of a receptor"
  [contents name receptor]
  (dosync (alter (@contents :receptors) assoc name receptor)))

(defn- unserialize-dump
  "convert an object from dump into a receptor"
  [{:keys [name- type- receptors-], :as dump-obj}]
  (let [receptor (receptor-factory name- type-)
        contents (:contents receptor)
        attributes (dissoc dump-obj :name- :type- :receptors-)]
    (if (not (empty? receptors-))
      (dorun (map #(let [r (unserialize-dump %)
                         rn (:self @(:contents r))]
                     (add-receptor-to-contents contents rn r) ) receptors-)))
    ;; create refs for each of the attributes so they can be mutable
    ;; in the receptor
    (dosync (alter contents merge (into {} (map (fn [[key val]] [key (ref val)]) attributes)) ))
    receptor
    )
  )

(defn serialize-receptor
  "convert a receptor into a string that can be passed to unserialze-receptor"
  [receptor]
  (str (dump-receptor receptor)))

(defn unserialize-receptor
  "convert a serialzed receptor into a receptor"
  [receptor-str]
  (unserialize-dump (with-in-str receptor-str (read))))

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
  "Utility function to resolve an address to :self or to a contained receptor in the contents"
  [{:keys [receptors self], :as contents} {:keys [id aspect], :as address}]
  (let  [[head & rest] (.split #"\." id)]
    [(if (= self head) :self (@receptors head))
     (if (nil? rest) address (assoc address :id (str/join "." rest)))])
  )

(defn aspect-receive-dispatch
  "Dispatches to aspect-receive calls based on the class of the recptor or a force"
  ([this signal contents klass]
      (if klass klass (class this)))
  ([this signal contents] (aspect-receive-dispatch this signal contents nil))
  )

(declare make-receptor-from-signal)

(defn do-conjure
  "conjure a recptor into the contents ref of a receptor"
  [contents body]
  (let [{:keys [name]} body]
    (add-receptor-to-contents contents name (make-receptor-from-signal body))
    "created"))

(defn do-ping
  "return a ping string"
  [from body]
  (str "I got '" body "' from " (humanize-address from))
  )

(defmulti receptor-aspects aspect-receive-dispatch)

;; The default dispatch for receptor-aspects is the "base" receptor.
(defmethod receptor-aspects :default
  [this signal contents & _]
  (let [{:keys [from to body]} signal]
    (condp = (:aspect to)
        
        ;; respond to a ping request
        :ping (do-ping from body) 

        ;; add a sub receptor into the receptors contents
        :conjure (do-conjure contents body)
        
        ;; otherwise throw an error
        (throw-bad-aspect to))))

(defn receptor-receive
  "Receive a signal, and either route it to a sub-receptors, or dispatch to the aspect handler."
  [this signal contents] 
  (let [parsed-signal (parse-signal signal)
        {:keys [from to body]} parsed-signal
        [destination-receptor resolved-address] (resolve-address @contents to)]
    (if (= destination-receptor :self)
      (receptor-aspects this parsed-signal contents)
      (if (nil? destination-receptor)
          (throw (RuntimeException. (str "No route to '" (humanize-address to) "'")))
          (receive destination-receptor (assoc parsed-signal :to resolved-address))))))

(defn receptor-scape
  "Utility function to return a receptor's scape"
  [receptor scape]
  (let [contents (:contents receptor)
        scapes (:scapes @contents)
        scape-fn (if scapes (scape @scapes) nil)]
    (if scape-fn
      scape-fn
      (throw (RuntimeException. (str "Unknown scape '" scape "' in " (:self @contents))))))
  )

(defn receptor-resolve
  "Resolve a scape key to a receptor address"
  [receptor scape key]
  ((receptor-scape receptor scape) key))

(defn scape-keys
  "Utility function to return a lazy list of all the keys in a scape"
  [receptor scape]
  (keys (receptor-scape receptor scape)))

(defrecord Receptor [contents]
  Ceptr
  (get-aspects [this] #{:conjure :ping})
  (receive [this signal] (receptor-receive this signal contents)))

(defn make-contents
  "Utility function to create an empty contents ref for a new receptor"
  ( [self-name attributes]
      (let [ contents {:receptors (ref {}), :self self-name}]
        (ref (if attributes (merge contents attributes) contents))))
  ( [self-name] (make-contents self-name nil)))

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
  (Receptor. (make-contents name)))


(defrecord ObjectReceptor [contents]
  Ceptr
  (get-aspects [this] #{:ping :conjure})
  (receive [this signal] 
     (let [{:keys [from to body error]} (validate-signal this signal true)]
      (do-ping from body)
      )))

(defn make-object
  "Utility function to create an empty object receptor"
  [name]
  (ObjectReceptor. (make-contents name)))

(defrecord ServerReceptor [contents]
  Ceptr
  (get-aspects [this] #{:ping :conjure :users})
  (receive [this signal] (receptor-receive this signal contents))
  )

(defn make-server
  "Sever factory

servers receive the following signals:
     aspect: 
       body: {:name <receptor-name>, :type <receptor-type>, ...<other keys as defined by the recptor type>}
    returns: \"\" if successful
"
  [name]
  (ServerReceptor. (make-contents name)))

(defmethod receptor-aspects anansi.receptor.ServerReceptor
  [this signal contents]
  (let [{:keys [to body]} signal
        ]
    (condp = (:aspect to)
        :users (str (vec (keys @user-streams)))
        (receptor-aspects this signal contents :default))))

(defrecord RoomReceptor [contents]
  Ceptr
  (get-aspects [this] #{:ping :conjure :describe :enter :leave :scape :pass-object})
  (receive [this signal] (receptor-receive this signal contents)))

(defn find-receptor
  "Utility function to lookup up a receptor in the contents by address"
  [contents address]
  (@(@contents :receptors) address)
  )

(defn remove-receptor
  "Utility function to remove a receptor from the contents by address"
  [contents address]
  (alter (@contents :receptors) dissoc address)
  )

(defn calculate-angles
  "calculate angles scape from the seat scape"
  [seat-scape]
  (let [size (count seat-scape)]
    (if (> size 0)
      (into (sorted-map) (zipmap (range 0 360 (/ 360 size)) (vals seat-scape)))
      (sorted-map))))

(declare make-person)
(defmethod receptor-aspects anansi.receptor.RoomReceptor
  [this signal contents]
  (let [{:keys [to body]} signal
        people-ref (@contents :people)
        scapes-ref (@contents :scapes)
        ]
    (condp = (:aspect to)
        :describe (str (vec (map :name (vals @people-ref))))
        :enter (let [{:keys [person]} body
                     {:keys [name]} person
                     person-address (sanitize-for-address name)
                     person-receptor (find-receptor contents person-address)
                     ]
                 (if person-receptor
                   (str name " is already in the room")
                   (dosync (alter people-ref assoc person-address {:name name})
                           (let [seat-scape (:seat @scapes-ref)
                                 new-seat-scape (assoc seat-scape (count seat-scape) person-address)]
                             (alter scapes-ref assoc :seat new-seat-scape)
                             (alter scapes-ref assoc :angle (calculate-angles new-seat-scape)))
                           (do-conjure contents {:name person-address, :attributes {:name name} :type "Person"})
                           (str "entered as " person-address)) 
                   )
                 )
        :leave (let [{:keys [person-address]} body
                     person-receptor (find-receptor contents person-address)]
                 (if (nil? person-receptor)
                   (str person-address " is not in the room")
                   (dosync (remove-receptor contents person-address)
                           (alter people-ref dissoc person-address)
                           (let [seat-scape (:seat @scapes-ref)
                                 old-vals (filter #(not= person-address %) (vals seat-scape))
                                 new-seat-scape (into (sorted-map) (zipmap (range (count old-vals)) old-vals))]
                             (alter scapes-ref assoc :seat new-seat-scape) 
                             (alter scapes-ref assoc :angle (calculate-angles new-seat-scape)))
                           (str person-address " left"))))
        :pass-object nil
        (receptor-aspects this signal contents :default))))

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
  (RoomReceptor. (make-contents name {:scapes (ref {:seat (sorted-map),:angle (sorted-map)}) :objects (ref {}), :people (ref {})})))

(defrecord PersonReceptor [contents]
  Ceptr
  (get-aspects [this] #{:ping :conjure :get-attributes :set-attributes :receive-object :release-object})
  (receive [this signal] 
    (let [parsed-signal (parse-signal signal)
          {:keys [from to body]} parsed-signal
          attributes (:attributes @contents)]
      (condp = (:aspect to)
        :get-attributes (let [{:keys [keys]} body] (if (nil? keys) @attributes (select-keys @attributes keys))) 
        :set-attributes (dosync (alter attributes merge body))
        :receive-object "not-implemented"
        :release-object "not-implemented"
        (receptor-aspects this signal contents :default)
        )
)))

(defn make-person
  "Utility function to create a person receptor"
  ([name] (make-person name {}))
  ([name attributes]
     (PersonReceptor. (make-contents name {:attributes (ref attributes)}))))

(defn receptor-factory
  "create a new receptor"
  [name type & args]
  (condp = type
        "Object" (make-object name)
        "Receptor" (make-receptor name)
        "Person" (make-person name)
        "Room" (make-room name)
      (throw (RuntimeException. (str "Unknown receptor type: '" type "'")))
      )
  )

;; FIXME, other arguments in the body aren't parsed, checked or anything
(defn make-receptor-from-signal
  "Create a new receptor based on the parameters specified in the body of the signal"
  [body]
  (let [{:keys [type name]} body]
    (receptor-factory name type)
))

