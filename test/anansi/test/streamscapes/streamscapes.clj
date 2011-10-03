(ns anansi.test.streamscapes.streamscapes
  (:use [anansi.streamscapes.streamscapes] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]   
        [anansi.receptor.user]
        [anansi.streamscapes.ident :only [ident-def]] ; this
        [anansi.streamscapes.droplet :only [droplet-def]]        
        [anansi.streamscapes.channel :only [get-deliverer-bridge get-receiver-bridge get-controller]]
        [anansi.streamscapes.channels.email-bridge-out :only [channel->deliver email-bridge-out-def]]
        [anansi.streamscapes.channels.email-bridge-in :only [email-bridge-in-def]]
        [anansi.streamscapes.channels.irc-controller :only [channel->control]]
        [anansi.streamscapes.channels.irc-bridge-in]
        [anansi.streamscapes.channels.irc-controller :only [irc-controller-def]])
  (:use [midje.sweet])
  (:use [clojure.test])
)

  (set! *print-level* 6)
(let [m (make-receptor user-def nil "eric")
      u (make-receptor user-def nil "zippy")
      r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
      droplet-channels (get-scape r :droplet-channel)
      channels (get-scape r :channel)
      ids (get-scape r :id)
      ] 
  (facts "scaping relationships"
    (scape-relationship (get-scape r :channel) :key) => :name
    (scape-relationship (get-scape r :channel) :address) => :address
    (scape-relationship (get-scape r :delivery) :key) => :streamscapes-channel-time-map
    (scape-relationship (get-scape r :delivery) :address) => :droplet-address
    (scape-relationship (get-scape r :id) :key) => :droplet-address
    (scape-relationship (get-scape r :id) :address) => :streamscapes-channel-address
    (scape-relationship droplet-channels :key) => :droplet-address
    (scape-relationship droplet-channels :address) => :streamscapes-channel)

  (deftest streamscapes
    (testing "initialization"
      (is (= [(address-of m)] (s-> address->resolve (get-scape r :matrice) :matrice)))
      (is (= {:datax "x"} (contents r :data)))
      )
    (testing "identity"
      (let [identity-address1 (s-> matrice->identify r {:identifiers {:email "eric@example.com" :ssn 987564321} :attributes {:name "Eric" :eye-color "blue"}})
            identity-address2 (s-> matrice->identify r {:identifiers {:email "eric@otherexample.com" :ssn 123456789} :attributes {:name "Eric" :eye-color "green"}})
            ident-names (get-scape r :ident-name)
            email-idents (get-scape r :email-ident)
            ssn-idents (get-scape r :ssn-ident)
            ident-eye-colors (get-scape r :ident-eye-color)
            ]
        (is (= (scape-relationship ident-names :key) :ident-address))
        (is (= (scape-relationship ident-names :address) :name-attribute))
        (is (= (scape-relationship ssn-idents :key) :ssn-identifier))
        (is (= (scape-relationship ssn-idents :address) :ident-address))

        (is (= identity-address1) (find-identities r {:email "eric@example.com"}))
        (is (= identity-address1 (s-> key->resolve email-idents "eric@example.com")))
        (is (= identity-address1 (s-> key->resolve ssn-idents 987564321)))
        (is (= [identity-address2] (s-> address->resolve ident-eye-colors "green")))
        (is (= [identity-address1 identity-address2] (s-> address->resolve ident-names "Eric")))
        (is (= identity-address2 (s-> key->resolve email-idents "eric@otherexample.com")))
        (is (thrown-with-msg? RuntimeException #"identity already exists for identifiers: eric@example.com" (s-> matrice->identify r {:identifiers {:email "eric@example.com"}})))
        (is (= [identity-address1 identity-address2] (do-identify r {:identifiers {:email "eric@example.com" :ssn 123456789}} false)))
        (is (= identity-address1 (do-identify r {:identifiers { :email "eric@example.com"}} false)))
        (is (= identity-address1 (do-identify r {:identifiers { :email "eric@example.com" :irc "zippy314"}} false)))
        (is (= identity-address1 (do-identify r {:identifiers { :irc "zippy314"}} false)))
        )
      )
    (testing "droplets"
      (let [droplet-address (s-> matrice->incorporate r {:id "some-unique-id" :from "from-addr" :to "to-addr" :channel :some-channel :envelope {:part1 "address of part1 grammar"} :content {:part1 "part1 content"}})
            d (get-receptor r droplet-address)]
        (are [x y] (= x y)
             (contents d :id) "some-unique-id"
             (contents d :from) "from-addr"
             (contents d :to) "to-addr"
             (contents d :channel) :some-channel
             (contents d :envelope) {:part1 "address of part1 grammar"}
             (contents d :content) {:part1 "part1 content"}
             (address-of d) droplet-address
             :some-channel (s-> key->resolve droplet-channels droplet-address)
             "some-unique-id" (s-> key->resolve ids droplet-address)
             [droplet-address] (s-> address->resolve droplet-channels :some-channel)
             [droplet-address] (s-> address->resolve ids "some-unique-id")
             )))
    
    (testing "streamscapes receive"
      (is (thrown-with-msg? RuntimeException #"channel not found: fish" (s-> streamscapes->receive r {:channel "fish"}))))
    
    (testing "email-channel"
      (let [channel-address (s-> matrice->make-channel r {:name :email-stream
                                                          :receptors {
                                                                      email-bridge-in-def {:role :receiver :params {:attributes {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3"}}}
                                                                      email-bridge-out-def {:role :deliverer :signal channel->deliver :params {:attributes {:host "mail.harris-braun.com" :account "eric@harris-braun.com" :password "some-password" :protocol "smtps" :port 25}}}}
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
                                                                      irc-controller-def {:role :controller :signal channel->control :params {:attributes {:host "irc.freenode.net" :port 6667 :user "Eric" :nick "zippy31415"}}}}
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
        (is (= #{ :an-irc-channel, :email-stream, :freenode, :irc-stream, :email} (set (keys (:map (receptor-state (get-scape r :channel) true)))))))
      
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
  
  )

