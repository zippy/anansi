(ns anansi.test.streamscapes.streamscapes
  (:use [anansi.streamscapes.streamscapes] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]   
        [anansi.receptor.user]
        [anansi.receptor.host :only [host-def]]
        [anansi.streamscapes.contact :only [contact-def]] ; this
        [anansi.streamscapes.droplet :only [droplet-def]]        
        [anansi.streamscapes.channel :only [get-deliverer-bridge get-receiver-bridge get-controller]]
        [anansi.streamscapes.channels.email-bridge-out :only [channel->deliver email-bridge-out-def]]
        [anansi.streamscapes.channels.email-bridge-in :only [email-bridge-in-def]]
        [anansi.streamscapes.channels.local-bridge-out :only [local-bridge-out-def]]
        [anansi.streamscapes.channels.local-bridge-in :only [local-bridge-in-def]]
        [anansi.streamscapes.channels.email-controller :only [email-controller-def]]
        [anansi.streamscapes.channels.irc-controller :only [channel->control]]
        [anansi.streamscapes.channels.irc-bridge-in :only [irc-bridge-in-def]]
        [anansi.streamscapes.channels.irc-bridge-out :only [irc-bridge-out-def]]
        [anansi.streamscapes.channels.irc-controller :only [irc-controller-def]]
        [anansi.streamscapes.channels.twitter-bridge-in :only [twitter-bridge-in-def]]
        [anansi.streamscapes.channels.twitter-controller :only [twitter-controller-def]]
        [anansi.streamscapes.channels.socket-in :only [socket-in-def]]
        [anansi.streamscapes.channels.socket-controller :only [socket-controller-def]])
  (:use [midje.sweet])
  (:use [clojure.test])
)

  (set! *print-level* 6)
(let [h (make-receptor host-def nil {})
      m (make-receptor user-def h "eric")
      u (make-receptor user-def h "zippy")
      r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
      droplet-channels (get-scape r :droplet-channel)
      channels (get-scape r :channel)
      channel-types (get-scape r :channel-type)
      ids (get-scape r :id)
      ] 
  (facts "scaping relationships"
    (scape-relationship (get-scape r :channel) :key) => "channel-name"
    (scape-relationship (get-scape r :channel) :address) => "channel-address"
    (scape-relationship (get-scape r :delivery) :key) => "timestamp"
    (scape-relationship (get-scape r :delivery) :address) => "droplet-address"
    (scape-relationship (get-scape r :id) :key) => "droplet-address"
    (scape-relationship (get-scape r :id) :address) => "streamscapes_channel_address"
    (scape-relationship droplet-channels :key) => "droplet-address"
    (scape-relationship droplet-channels :address) => "channel-address")
  
  (facts  "about initialization"
    (s-> address->resolve (get-scape r :matrice) :matrice) => [(address-of m)]
    (contents r :data) => {:datax "x"}
    )
  (facts "about contacts"
    (let [contact-address1 (s-> matrice->create-contact r {:identifiers {:email-address "eric@example.com" :ssn-address 987564321} :attributes {:name "Eric" :eye-color "blue"}})
          contact-address2 (s-> matrice->create-contact r {:identifiers {:email-address "eric@otherexample.com" :ssn-address 123456789} :attributes {:name "Eric" :eye-color "green"}})
          contact-address3 (s-> matrice->create-contact r {:identifiers {:email-address "eric@yetanotherotherexample.com"}})
          contact-names (get-scape r :contact-name)
          email-contacts (get-scape r :email-address-contact)
          ssn-contacts (get-scape r :ssn-address-contact)
          contact-eye-colors (get-scape r :contact-eye-color)
          ]
      (scape-relationship contact-names :key) => :contact-address
      (scape-relationship contact-names :address) => :name-attribute
      (scape-relationship ssn-contacts :key) => :ssn-address-identifier
      (scape-relationship ssn-contacts :address) => :contact-address

      (into [] (find-contacts r {:email-address "eric@example.com"})) => [contact-address1]
      (s-> key->resolve email-contacts "eric@example.com") => contact-address1
      (s-> key->resolve ssn-contacts 987564321) => contact-address1
      (s-> address->resolve contact-eye-colors "green") => [contact-address2]
      (s-> address->resolve contact-names "Eric") => [contact-address1 contact-address2]
      (s-> key->resolve email-contacts "eric@otherexample.com") = contact-address2
      (s-> matrice->identify r {:identifiers {:email-address "eric@example.com"}}) => (throws RuntimeException "contact already exists for identifiers: eric@example.com")
      (do-identify r {:identifiers {:email-address "eric@example.com" :ssn-address 123456789}} false) => [contact-address1 contact-address2]
      (do-identify r {:identifiers {:email-address "eric@example.com"}} false) => contact-address1
      (do-identify r {:identifiers {:email-address "eric@example.com" :irc-address "zippy314"}} false) => contact-address1
      (do-identify r {:identifiers {:irc-address "zippy314"}} false) => contact-address1
      (s-> key->resolve contact-names contact-address3) => "\"eric@yetanotherotherexample.com\""

      (facts "about creating contacts"
        (s-> matrice->create-contact r {:identifiers {:email-address "eric@example.com"}}) => (throws RuntimeException "There are contacts already identified by one or more of: eric@example.com")
        (s-> address->resolve email-contacts contact-address1) => (just "eric@example.com")
        (let [c (s-> matrice->create-contact r {:identifiers {:email-address "eric@example.com"} :override-uniquness-check true})]
          (class c) => java.lang.Integer
          (class contact-address1) => java.lang.Integer
          (= c contact-address1) => false
          ))
        
      ;; identifiers only identify one contact, so the above create contact switched
      ;; "eric@example.com" to point to the newly created c contact
      (fact (s-> address->resolve email-contacts contact-address1) => empty?)
        
      (facts "about scaping contacts"
        (s-> matrice->scape-contact r {:address 999}) => (throws RuntimeException "No such contact: 999")
        (s-> matrice->scape-contact r {:address (address-of contact-names)}) => (throws RuntimeException (str "No such contact: " (address-of contact-names)))
        (s-> matrice->scape-contact r {:address contact-address1 :identifiers {:email-address "eric@other-address.com"}})
        (s-> address->resolve email-contacts contact-address1) => (just "eric@other-address.com")
        (s-> matrice->scape-contact r {:address contact-address1 :attributes {:name "Bugsy"}})
        (s-> key->resolve contact-names contact-address1) => "Bugsy"
        )
      (facts "about deleting contacts"
        (s-> matrice->delete-contact r {:address 999})  => (throws RuntimeException "No such contact: 999")
        (s-> matrice->delete-contact r {:address (address-of contact-names)}) => (throws RuntimeException (str "No such contact: " (address-of contact-names)))
        (s-> matrice->delete-contact r {:address contact-address1})
        (get-receptor r contact-address1) => nil
        (s-> address->resolve email-contacts contact-address1) => []
          
        )
      )
    )
  (facts "about scaping"
    (let [s-addr (s-> setup->new-scape r {:name :fish :relationship {:key :fish-name, :address :address}})]
      (class s-addr) => java.lang.Integer
      (scape-relationship (get-scape r :fish) :key) => :fish-name
      (scape-relationship (get-scape r :fish) :address) => :address
      (s-> scape->set r {:name :fish :key "trout" :address 2}) => nil
      (s-> key->resolve (get-scape r :fish) "trout") => 2
      (s-> scape->delete r {:name :fish :key "trout"}) => nil
      (s-> key->resolve (get-scape r :fish) "trout") => nil
      (s-> setup->rename-scape r {:name :fish :new-name :ichthoid})
      (get-scape r :fish) => (throws RuntimeException ":fish scape doesn't exist")
      (address-of (get-scape r :ichthoid)) => s-addr
      (s-> setup->delete-scape r {:name :ichthoid})
      (get-scape r :ichthoid) => (throws RuntimeException ":ichthoid scape doesn't exist")
      ))
  (facts "about droplets"
    (let [sc (s-> matrice->make-channel r {:name :some-channel})
          x (--> key->set r (get-scape r :channel-type) sc :email)
          droplet-address (s-> matrice->incorporate r {:id "some-unique-id" :from "from-addr" :to "to-addr" :channel :some-channel :envelope {:subject "text/plain" :body "text/html"} :content {:subject "subj content" :body "<b> hello! </b>"}})
          d (get-receptor r droplet-address)
          droplet-grooves (get-scape r :droplet-grooves)]
        
      (s-> key->resolve droplet-grooves droplet-address) => [:subject-body-message] 
      (contents d :id) => "some-unique-id"
      (contents d :from) => "from-addr"
      (contents d :to) => "to-addr"
      (contents d :channel) => :some-channel
      (contents d :envelope) => {:subject "text/plain" :body "text/html"}
      (contents d :content) => {:subject "subj content" :body "<b> hello! </b>"}
      (address-of d) => droplet-address
      (s-> key->resolve droplet-channels droplet-address) => sc
      (s-> key->resolve ids droplet-address) => "some-unique-id"
      (s-> address->resolve droplet-channels sc) => [droplet-address]
      (s-> address->resolve ids "some-unique-id") => [droplet-address]

      (s-> matrice->discorporate r {:droplet-address droplet-address}) => nil
      (s-> key->resolve droplet-channels droplet-address) => nil
      (s-> key->resolve ids droplet-address) => nil
      (s-> address->resolve droplet-channels sc) => []
      (s-> address->resolve ids "some-unique-id") => []
      (get-receptor r droplet-address) => nil
      (s-> matrice->discorporate r {:droplet-address 99}) => (throws RuntimeException "no such droplet: 99")
      ))
    
  (facts "abut streamscapes receive"
    (s-> streamscapes->receive r {:channel "fish"}) => (throws RuntimeException "channel not found: fish"))
    
  (facts "about email-channel"
    (let [channel-address (s-> matrice->make-channel r {:name :email-stream
                                                        :receptors {
                                                                    email-bridge-in-def {:role :receiver :params {:attributes {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3"}}}
                                                                    email-bridge-out-def {:role :deliverer :signal ["anansi.streamscapes.channels.email-bridge-out" "channel" "deliver"] :params {:attributes {:host "mail.harris-braun.com" :account "eric@harris-braun.com" :password "some-password" :protocol "smtps" :port 25}}}}
                                                        })
          cc (get-receptor r channel-address)
          [out-bridge-address delivery-signal] (get-deliverer-bridge cc)
          [in-bridge-address receive-signal] (get-receiver-bridge cc)
          db (get-receptor cc out-bridge-address)]
      (find-channel-by-name r :email-stream) => cc
      (rdef db :fingerprint) => :anansi.streamscapes.channels.email-bridge-out.email-bridge-out
      (contents db :host) => "mail.harris-braun.com"
      (rdef (get-receptor cc in-bridge-address) :fingerprint) => :anansi.streamscapes.channels.email-bridge-in.email-bridge-in))
  (facts "about irc-channel"
    (let [channel-address (s-> matrice->make-channel r {:name :irc-stream
                                                        :receptors {irc-bridge-in-def {:role :receiver :params {} }
                                                                    irc-controller-def {:role :controller :signal ["anansi.streamscapes.channels.irc-controller" "channel" "control"] :params {:attributes {:host "irc.freenode.net" :port 6667 :user "Eric" :nick "zippy31415"}}}}
                                                        })
          cc (get-receptor r channel-address)
          [controller-address control-signal] (get-controller cc)
          [out-bridge-address delivery-signal] (get-deliverer-bridge cc)
          [in-bridge-address receive-signal] (get-receiver-bridge cc)
          db (get-receptor cc controller-address)]
      (find-channel-by-name r :irc-stream) => cc
      (rdef db :fingerprint) => :anansi.streamscapes.channels.irc-controller.irc-controller
      (contents db :host) => "irc.freenode.net"
      (contents db :port) => 6667
      (rdef (get-receptor cc in-bridge-address) :fingerprint) => :anansi.streamscapes.channels.irc-bridge-in.irc-bridge-in
      (set (keys (:map (receptor-state (get-scape r :channel) true)))) => #{:some-channel :email-stream :irc-stream}))
  
  (facts "about new twitter channel"
    (let [channel-address (s-> setup->new-channel r {:type :twitter, :name :twitterx, :search-query "@zippy314"})
          cc (get-receptor r channel-address)
          [in-bridge-address receive-signal] (get-receiver-bridge cc)
          [controller-address controller-signal] (get-controller cc)]
      (receptor-state (get-receptor cc in-bridge-address) false) => (contains {:fingerprint :anansi.streamscapes.channels.twitter-bridge-in.twitter-bridge-in })
      (find-channel-by-name r :twitterx) => cc
      (receptor-state (get-receptor cc controller-address) false) => (contains {:fingerprint :anansi.streamscapes.channels.twitter-controller.twitter-controller :search-query "@zippy314"})
      ))

  (facts "about new socket channel"
    (let [channel-address (s-> setup->new-channel r {:type :socket, :name :a-socket, :port 31415})
          cc (get-receptor r channel-address)
          [in-bridge-address receive-signal] (get-receiver-bridge cc)
          [controller-address controller-signal] (get-controller cc)]
      (receptor-state (get-receptor cc in-bridge-address) false) => (contains {:fingerprint :anansi.streamscapes.channels.socket-in.socket-in })
      (find-channel-by-name r :a-socket) => cc
      (receptor-state (get-receptor cc controller-address) false) => (contains {:fingerprint :anansi.streamscapes.channels.socket-controller.socket-controller :port 31415})
      (s-> matrice->control-channel r {:name :a-socket :command :status}) => :closed
      (s-> matrice->control-channel r {:name :a-socket :command :close}) => (throws RuntimeException "Channel not open")
      (s-> matrice->control-channel r {:name :a-socket :command :open}) => nil
      (s-> matrice->control-channel r {:name :a-socket :command :status}) => :open
      (s-> matrice->control-channel r {:name :a-socket :command :close}) => nil
      (s-> matrice->control-channel r {:name :a-socket :command :status}) => :closed

      )
    )
  
  (facts "about new-channel"
    (s-> setup->new-channel r {:type :fish :name :fisher}) => (throws RuntimeException "channel type 'fish' not implemented")
    (str (s-> setup->new-channel r {:type "irc" :name :an-irc-channel})) =>  #"[0-9]+"
    (let [channel-address (s-> setup->new-channel r {:type :irc, :name :freenode, :host "irc.freenode.net", :port 6667, :user "Eric", :nick "zippy31415"})
          cc (get-receptor r channel-address)
          [controller-address control-signal] (get-controller cc)
          [in-bridge-address receive-signal] (get-receiver-bridge cc)
          db (get-receptor cc controller-address)]
      (rdef (get-receptor cc in-bridge-address) :fingerprint) => :anansi.streamscapes.channels.irc-bridge-in.irc-bridge-in
      (s-> key->resolve (get-scape r :channel-type) channel-address) => :irc
      (find-channel-by-name r :freenode) => cc
      (receptor-state db false) => (contains {:fingerprint :anansi.streamscapes.channels.irc-controller.irc-controller
                                             :user "Eric"
                                             :nick "zippy31415"
                                             :host "irc.freenode.net"
                                              :port 6667})))
  
  (facts "about new email channel"
    (let [channel-address (s-> setup->new-channel r {:type :email, :name :email,
                                                     :in {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3"}
                                                     :out {:host "mail.harris-braun.com" :account "eric@harris-braun.com" :password "some-password" :protocol "smtps" :port 25}})
          cc (get-receptor r channel-address)
          [out-bridge-address delivery-signal] (get-deliverer-bridge cc)
          [in-bridge-address receive-signal] (get-receiver-bridge cc)
          ]
      
      (receptor-state (get-receptor cc in-bridge-address) false) => (contains {:password "pass", :host "mail.example.com", :account "someuser", :protocol "pop3", :fingerprint :anansi.streamscapes.channels.email-bridge-in.email-bridge-in})
      (receptor-state (get-receptor cc out-bridge-address) false) => (contains {:protocol "smtps", :account "eric@harris-braun.com", :host "mail.harris-braun.com", :fingerprint :anansi.streamscapes.channels.email-bridge-out.email-bridge-out, :port 25, :password "some-password"})
      (find-channel-by-name r :email) => cc))


  (facts "about new streamscapes channel"
    (let [channel-address (s-> setup->new-channel r {:type :streamscapes, :name :streamscapes})
          cc (get-receptor r channel-address)
          [out-bridge-address delivery-signal] (get-deliverer-bridge cc)
          [in-bridge-address receive-signal] (get-receiver-bridge cc)]
      
      (receptor-state (get-receptor cc in-bridge-address) false) => (contains {:fingerprint :anansi.streamscapes.channels.local-bridge-in.local-bridge-in})
      (receptor-state (get-receptor cc out-bridge-address) false) => (contains {:fingerprint :anansi.streamscapes.channels.local-bridge-out.local-bridge-out,})
      (find-channel-by-name r :streamscapes) => cc))
  
  (facts "about control-channel"
    (s-> matrice->control-channel r {:name :fish :command :fish}) => (throws RuntimeException "channel not found: :fish")
    (s-> matrice->control-channel r {:name :freenode :command :fish}) => (throws RuntimeException "Unknown control command: :fish")
    (s-> matrice->control-channel r {:name :freenode :command :status}) => :closed
    (s-> matrice->control-channel r {:name :freenode :command :close}) => (throws RuntimeException "Channel not open")
; commented out in the name of speeding up the tests
;    (s-> matrice->control-channel r {:name :freenode :command :open}) => nil
;    (s-> matrice->control-channel r {:name :freenode :command :status}) => :open
;    (s-> matrice->control-channel r {:name :freenode :command :join :params {:channel "#ceptr"}}) => nil
;     (Thread/sleep 13000)
    )
  (facts "about update-channel"
    (let [cc (find-channel-by-name r :twitterx)
          [controller-address _] (get-controller cc)
          db (get-receptor cc controller-address)
          ]
      (s-> setup->update-channel r {:channel-address (address-of cc) :name :twittery :search-query "#new-search"})
      (= cc (find-channel-by-name r :twittery)) => true
      (find-channel-by-name r :twitterx) => (throws RuntimeException "channel not found: :twitterx")
      (--> key->resolve r (get-scape r :channel) :twitterx) => nil
      (--> key->resolve r (get-scape r :channel) :twittery) => (address-of cc)
      (contents db :search-query) => "#new-search"
      )
    (let [cc (find-channel-by-name r :freenode)
          [controller-address _] (get-controller cc)
          db (get-receptor cc controller-address)
          ]
      (s-> setup->update-channel r {:channel-address (address-of cc) :name :twittery}) => (throws RuntimeException "channel name 'twittery' already exists")
      (s-> setup->update-channel r {:channel-address (address-of cc) :name :new-irc-stream :host "newirc.freenode.net" :port 6669 :nick "newnick" :user "newuser"})
      (= cc (find-channel-by-name r :new-irc-stream)) => true
      (find-channel-by-name r :freenode) => (throws RuntimeException "channel not found: :freenode")
      (--> key->resolve r (get-scape r :channel) :freenode) => nil
      (--> key->resolve r (get-scape r :channel) :new-irc-stream) => (address-of cc)
      (contents db :host) => "newirc.freenode.net"
      (contents db :port) => 6669
      (contents db :nick) => "newnick"
      (contents db :user) => "newuser"
      )
    (let [cc (find-channel-by-name r :email)
          [in-address _] (get-receiver-bridge cc)
          [out-address _] (get-deliverer-bridge cc)
          in (get-receptor cc in-address)
          out (get-receptor cc out-address)
          ]
      (s-> setup->update-channel r {:channel-address (address-of cc) :name :new-email
                                    :in {:host "mail1.example.com" :account "newuser" :port 88 :password "newpass" :protocol "newprot"}
                                    :out {:host "mail1.example.com" :account "newuser" :port 88 :password "newpass" :protocol "newprot"}})
      (= cc (find-channel-by-name r :new-email)) => true
      (find-channel-by-name r :email) => (throws RuntimeException "channel not found: :email")
      (contents in :host) => "mail1.example.com"
      (contents in :account) => "newuser"
      (contents in :password) => "newpass"
      (contents in :protocol) => "newprot"
      (contents in :port) => 88
      (contents out :host) => "mail1.example.com"
      (contents out :account) => "newuser"
      (contents out :password) => "newpass"
      (contents out :protocol) => "newprot"
      (contents out :port) => 88
      (s-> setup->update-channel r {:channel-address (address-of cc) :name :new-email
                                    :in {:account "newuser2" :password ""}})
      (contents in :host) => "mail1.example.com"
      (contents in :account) => "newuser2"
      (contents in :password) => "newpass"
      (contents in :protocol) => "newprot"
      (contents in :port) => 88
      (contents out :host) => "mail1.example.com"
      (contents out :account) => "newuser"
      (contents out :password) => "newpass"
      (contents out :protocol) => "newprot"
      (contents out :port) => 88
      )
    )
  
  (facts "about deleting channels"
    (s-> setup->delete-channel r {:name :fish}) => (throws RuntimeException "channel not found: :fish")
    (let [fa (s-> setup->new-channel r {:type :twitter, :name :fish, :search-query "@fish"})
          a (s-> matrice->incorporate r {:id "some-unique-id2" :from "from-addr" :to "to-addr" :channel :fish :envelope {:message "text/plain"} :content {:message "the message"}})]
      (s-> address->resolve droplet-channels fa) => [a]
      (s-> key->resolve channels :fish) => fa
      (s-> key->resolve channel-types fa) => :twitter
      (s-> key->resolve ids a) =not=> nil
      (address-of (get-receptor r a)) => a
      (s-> setup->delete-channel r {:name :fish})
      (find-channel-by-name r :fish) => (throws RuntimeException "channel not found: :fish")
      (s-> address->resolve droplet-channels fa) => []
      (s-> key->resolve channels :fish) => nil
      (s-> key->resolve channel-types fa) => nil
      (s-> key->resolve ids a) => nil
      (get-receptor r a) => nil
      )
    )

  (facts "about restoring serialized receptor"
    (let [state (receptor-state r true)]
      state => (receptor-state (receptor-restore state nil) true)
      ))

  )


