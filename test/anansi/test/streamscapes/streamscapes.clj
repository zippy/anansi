(ns anansi.test.streamscapes.streamscapes
  (:use [anansi.streamscapes.streamscapes] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]   
        [anansi.receptor.user]
        [anansi.streamscapes.channel :only [get-deliverer-bridge get-receiver-bridge get-controller]]
        [anansi.streamscapes.channels.email-bridge-out :only [channel->deliver]]
        [anansi.streamscapes.channels.irc-controller :only [channel->control]]
        [anansi.streamscapes.channels.irc-bridge-in])
  (:use [clojure.test]))

(deftest streamscapes
  (let [m (receptor :user nil "eric" nil)
        u (receptor :user nil "zippy" nil)
        r (receptor :streamscapes nil (address-of m) "password" {:datax "x"})
        aspects (get-scape r :aspect)
        ids (get-scape r :id)
        ]
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
      (let [droplet-address (s-> matrice->incorporate r {:id "some-unique-id" :from "from-addr" :to "to-addr" :aspect :some-aspect :envelope {:part1 "address of part1 grammar"} :content {:part1 "part1 content"}})
            d (get-receptor r droplet-address)]
        (are [x y] (= x y)
             (contents d :id) "some-unique-id"
             (contents d :from) "from-addr"
             (contents d :to) "to-addr"
             (contents d :aspect) :some-aspect
             (contents d :envelope) {:part1 "address of part1 grammar"}
             (contents d :content) {:part1 "part1 content"}
             (address-of d) droplet-address
             :some-aspect (s-> key->resolve aspects droplet-address)
             "some-unique-id" (s-> key->resolve ids droplet-address)
             [droplet-address] (s-> address->resolve aspects :some-aspect)
             [droplet-address] (s-> address->resolve ids "some-unique-id")
             )))
    
    (testing "streamscapes receive"
      (is (thrown-with-msg? RuntimeException #"channel not found: fish" (s-> streamscapes->receive r {:aspect "fish"}))))
    
    (testing "email-channel"
      (let [channel-address (s-> matrice->make-channel r {:name :email-stream
                                                          :receptors {
                                                                      :email-bridge-in {:role :receiver :params {:host "mail.example.com" :account "someuser" :password "pass" :protocol "pop3"}}
                                                                      :email-bridge-out {:role :deliverer :signal channel->deliver :params {:host "mail.harris-braun.com" :account "eric@harris-braun.com" :password "some-password" :protocol "smtps" :port 25}}}
                                                          })
            cc (get-receptor r channel-address)
            [out-bridge-address delivery-signal] (get-deliverer-bridge cc)
            [in-bridge-address receive-signal] (get-receiver-bridge cc)
            db (get-receptor cc out-bridge-address)]
        (is (= cc (find-channel-by-name r :email-stream)))
        (is (= (:type @db) :email-bridge-out))
        (is (= (contents db :host) "mail.harris-braun.com"))
        (is (= (:type @(get-receptor cc in-bridge-address)) :email-bridge-in))))
    (testing "irc-channel"
      (let [channel-address (s-> matrice->make-channel r {:name :irc-stream
                                                          :receptors {:irc-bridge-in {:role :receiver :params {} }
                                                                      :irc-controller {:role :controller :signal channel->control :params {:host "irc.freenode.net" :port 6667 :user "Eric" :nick "zippy31415"}}}
                                                          })
            cc (get-receptor r channel-address)
            [controller-address control-signal] (get-controller cc)
            [out-bridge-address delivery-signal] (get-deliverer-bridge cc)
            [in-bridge-address receive-signal] (get-receiver-bridge cc)
            db (get-receptor cc controller-address)]
        (is (= cc (find-channel-by-name r :irc-stream)))
        (is (= (:type @db) :irc-controller))
        (is (= (contents db :host) "irc.freenode.net"))
        (is (= (contents db :port) 6667))
        (is (= (:type @(get-receptor cc in-bridge-address)) :irc-bridge-in)))
)))
