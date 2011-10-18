(ns anansi.test.streamscapes.streamscapes
  (:use [anansi.streamscapes.streamscapes] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]   
        [anansi.receptor.user]
        [anansi.receptor.host :only [host-def]]
        [anansi.streamscapes.ident :only [ident-def]] ; this
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
        [anansi.streamscapes.channels.twitter-controller :only [twitter-controller-def]])
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

  (deftest streamscapes
    (testing "initialization"
      (is (= [(address-of m)] (s-> address->resolve (get-scape r :matrice) :matrice)))
      (is (= {:datax "x"} (contents r :data)))
      )
    (testing "identity"
      (let [contact-address1 (s-> matrice->create-contact r {:identifiers {:email "eric@example.com" :ssn 987564321} :attributes {:name "Eric" :eye-color "blue"}})
            contact-address2 (s-> matrice->create-contact r {:identifiers {:email "eric@otherexample.com" :ssn 123456789} :attributes {:name "Eric" :eye-color "green"}})
            ident-names (get-scape r :ident-name)
            email-idents (get-scape r :email-ident)
            ssn-idents (get-scape r :ssn-ident)
            ident-eye-colors (get-scape r :ident-eye-color)
            ]
        (is (= (scape-relationship ident-names :key) :ident-address))
        (is (= (scape-relationship ident-names :address) :name-attribute))
        (is (= (scape-relationship ssn-idents :key) :ssn-identifier))
        (is (= (scape-relationship ssn-idents :address) :ident-address))

        (is (= contact-address1) (find-contacts r {:email "eric@example.com"}))
        (is (= contact-address1 (s-> key->resolve email-idents "eric@example.com")))
        (is (= contact-address1 (s-> key->resolve ssn-idents 987564321)))
        (is (= [contact-address2] (s-> address->resolve ident-eye-colors "green")))
        (is (= [contact-address1 contact-address2] (s-> address->resolve ident-names "Eric")))
        (is (= contact-address2 (s-> key->resolve email-idents "eric@otherexample.com")))
        (is (thrown-with-msg? RuntimeException #"identity already exists for identifiers: eric@example.com" (s-> matrice->identify r {:identifiers {:email "eric@example.com"}})))
        (is (= [contact-address1 contact-address2] (do-identify r {:identifiers {:email "eric@example.com" :ssn 123456789}} false)))
        (is (= contact-address1 (do-identify r {:identifiers { :email "eric@example.com"}} false)))
        (is (= contact-address1 (do-identify r {:identifiers { :email "eric@example.com" :irc "zippy314"}} false)))
        (is (= contact-address1 (do-identify r {:identifiers { :irc "zippy314"}} false)))

        (facts "about creating contacts"
          (s-> matrice->create-contact r {:identifiers {:email "eric@example.com"}}) => (throws RuntimeException "There are contacts already identified by one or more of: eric@example.com")
          (s-> address->resolve email-idents contact-address1) => (just "eric@example.com")
          (let [c (s-> matrice->create-contact r {:identifiers {:email "eric@example.com"} :override-uniquness-check true})]
            (class c) => java.lang.Integer
            (class contact-address1) => java.lang.Integer
            (= c contact-address1) => false
            ))
        
        ;; identifiers only identify one contact, so the above create contact switched
        ;; "eric@example.com" to point to the newly created c contact
        (fact (s-> address->resolve email-idents contact-address1) => empty?)
        
        (facts "about scaping contacts"
          (s-> matrice->scape-contact r {:address 999}) => (throws RuntimeException "No such contact: 999")
          (s-> matrice->scape-contact r {:address (address-of ident-names)}) => (throws RuntimeException (str "No such contact: " (address-of ident-names)))
          (s-> matrice->scape-contact r {:address contact-address1 :identifiers {:email "eric@other-address.com"}})
          (s-> address->resolve email-idents contact-address1) => (just "eric@other-address.com")
          (s-> matrice->scape-contact r {:address contact-address1 :attributes {:name "Bugsy"}})
          (s-> key->resolve ident-names contact-address1) => "Bugsy"
          )
        )
      )
    (facts "about scaping"
      (class (s-> setup->new-scape r {:name :fish :relationship {:key :fish-name, :address :address}})) => java.lang.Integer
      (scape-relationship (get-scape r :fish) :key) => :fish-name
      (scape-relationship (get-scape r :fish) :address) => :address
      (s-> scape->set r {:name :fish :key "trout" :address 2}) => nil
      (s-> key->resolve (get-scape r :fish) "trout") => 2
      )
    (testing "droplets"
      (let [sc (s-> matrice->make-channel r {:name :some-channel})
            x (--> key->set r (get-scape r :channel-type) sc :email)
            droplet-address (s-> matrice->incorporate r {:id "some-unique-id" :from "from-addr" :to "to-addr" :channel :some-channel :envelope {:subject "text/plain" :body "text/html"} :content {:subject "subj content" :body "<b> hello! </b>"}})
            d (get-receptor r droplet-address)]
        (fact (s-> key->resolve (get-scape r :droplet-grooves) droplet-address) => [:subject-body-message] )
        (are [x y] (= x y)
             (contents d :id) "some-unique-id"
             (contents d :from) "from-addr"
             (contents d :to) "to-addr"
             (contents d :channel) :some-channel
             (contents d :envelope) {:subject "text/plain" :body "text/html"}
             (contents d :content) {:subject "subj content" :body "<b> hello! </b>"}
             (address-of d) droplet-address
             (s-> key->resolve droplet-channels droplet-address) sc
             "some-unique-id" (s-> key->resolve ids droplet-address)
             [droplet-address] (s-> address->resolve droplet-channels sc)
             [droplet-address] (s-> address->resolve ids "some-unique-id")
             )))
    
    (testing "streamscapes receive"
      (is (thrown-with-msg? RuntimeException #"channel not found: fish" (s-> streamscapes->receive r {:channel "fish"}))))
    
    (testing "email-channel"
      (let [channel-address (s-> matrice->make-channel r {:name :email-stream
                                                          :receptors {
                                                                      email-bridge-in-def {:role :receiver :params {:attributes {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3"}}}
                                                                      email-bridge-out-def {:role :deliverer :signal ["anansi.streamscapes.channels.email-bridge-out" "channel" "deliver"] :params {:attributes {:host "mail.harris-braun.com" :account "eric@harris-braun.com" :password "some-password" :protocol "smtps" :port 25}}}}
                                                          })
            cc (get-receptor r channel-address)
            [out-bridge-address delivery-signal] (get-deliverer-bridge cc)
            [in-bridge-address receive-signal] (get-receiver-bridge cc)
            db (get-receptor cc out-bridge-address)]
        (is (= cc (find-channel-by-name r :email-stream)))
        (is (= (rdef db :fingerprint) :anansi.streamscapes.channels.email-bridge-out.email-bridge-out))
        (is (= (contents db :host) "mail.harris-braun.com"))
        (is (= (rdef (get-receptor cc in-bridge-address) :fingerprint) :anansi.streamscapes.channels.email-bridge-in.email-bridge-in))))
    (testing "irc-channel"
      (let [channel-address (s-> matrice->make-channel r {:name :irc-stream
                                                          :receptors {irc-bridge-in-def {:role :receiver :params {} }
                                                                      irc-controller-def {:role :controller :signal ["anansi.streamscapes.channels.irc-controller" "channel" "control"] :params {:attributes {:host "irc.freenode.net" :port 6667 :user "Eric" :nick "zippy31415"}}}}
                                                          })
            cc (get-receptor r channel-address)
            [controller-address control-signal] (get-controller cc)
            [out-bridge-address delivery-signal] (get-deliverer-bridge cc)
            [in-bridge-address receive-signal] (get-receiver-bridge cc)
            db (get-receptor cc controller-address)]
        (is (= cc (find-channel-by-name r :irc-stream)))
        (is (= (rdef db :fingerprint) :anansi.streamscapes.channels.irc-controller.irc-controller))
        (is (= (contents db :host) "irc.freenode.net"))
        (is (= (contents db :port) 6667))
        (is (= (rdef (get-receptor cc in-bridge-address) :fingerprint) :anansi.streamscapes.channels.irc-bridge-in.irc-bridge-in))
        (is (= #{:an-irc-channel, :email-stream, :freenode, :irc-stream, :email, :twitterx, :streamscapes, :some-channel} (set (keys (:map (receptor-state (get-scape r :channel) true)))))))
      
      ))

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
  (facts "about new twitter channel"
    (let [channel-address (s-> setup->new-channel r {:type :twitter, :name :twitterx, :search-query "@zippy314"})
          cc (get-receptor r channel-address)
          [in-bridge-address receive-signal] (get-receiver-bridge cc)
          [controller-address controller-signal] (get-controller cc)]
      (receptor-state (get-receptor cc in-bridge-address) false) => (contains {:fingerprint :anansi.streamscapes.channels.twitter-bridge-in.twitter-bridge-in })
      (find-channel-by-name r :twitterx) => cc
      (receptor-state (get-receptor cc controller-address) false) => (contains {:fingerprint :anansi.streamscapes.channels.twitter-controller.twitter-controller :search-query "@zippy314"})
      ))
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

  (facts "about restoring serialized receptor"
    (let [state (receptor-state r true)]
      state => (receptor-state (receptor-restore state nil) true)
      ))

  )

(facts "about grammar-match?"
  (grammar-match? {:subject "text/plain" :body "text/html"} {:subject "text/plain" :body "text/plain"} {:subject "Hi there" :body "yo!"}) => true
  (grammar-match? {:subject "text/plain" :body "text/html"} {:subject "text/plain"} {:subject "Hi there"}) => false
  (grammar-match? {:message {"text" ["yo!"]}} {:message "text/plain"} {:message "yo!"}) => true
  (grammar-match? {:message {"text" ["yo!"]}} {:message "text/plain"} {:message "boink"}) => false
  (grammar-match? {:message {"text" ["yo!"]}} {:message "img/jpg"} {:message "yo!"}) => false
  (let [punkmoney {:subject {"text" ["Punkmoney Promise"]}
                   :body {"text"
                          ["I promise to pay (.*), on demand, ([^.]+)\\. Expires in (.*)\\."
                           {:payee 1
                            :promised-good 2
                            :expiration 3}]}}]
    (grammar-match? punkmoney
                    {:subject "text/plain" :body "text/plain"}
                    {:subject "Punkmoney Promise" :body "I promise to pay eric@example.com, on demand, some squids. Expires in 1 year."}) => true
    (grammar-match? punkmoney
                    {:subject "text/plain" :body "text/plain"}
                    {:subject "Punkmoney" :body "I promise to pay eric@example.com, on demand, some squids. Expires in 1 year."}) => false                
                    )
  

;;  (grammar-match? {:subject "text/plain" :body [#"yo!"]} {:subject "text/plain" :body "text/plain"} {:subject "Hi there" :body "yo!"}) => true
  )


