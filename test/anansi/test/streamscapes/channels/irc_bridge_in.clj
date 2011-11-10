(ns anansi.test.streamscapes.channels.irc-bridge-in
  (:use [anansi.streamscapes.channels.irc-bridge-in] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.receptor.host :only [host-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def]]
        [anansi.streamscapes.contact :only [contact-def]])
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest irc-bridge-in
  (let [m (make-receptor user-def nil "eric")
        h (make-receptor host-def nil {})
        r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor contact-def r {:attributes {:name "Eric"}})
        art (make-receptor contact-def r {:attributes {:name "Art"}})
        ceptr-channel (make-receptor contact-def r {:attributes {:name "ceptr-channel"}})
        cc-addr (s-> matrice->make-channel r {:name :irc-stream})
        cc (get-receptor r cc-addr)
        b (make-receptor irc-bridge-in-def cc {})
        irc-contacts (get-scape r :irc-address-contact true)]
    (--> key->set r (get-scape r :channel-type) cc-addr :irc)
    (--> key->set b irc-contacts "zippy" (address-of eric))
    (--> key->set b irc-contacts "art" (address-of art))
    (--> key->set b irc-contacts "#ceptr" (address-of ceptr-channel))

    (fact
      (receptor-state b false) => (contains {:fingerprint :anansi.streamscapes.channels.irc-bridge-in.irc-bridge-in}))
    
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    
    (testing "internal functions: handle-message (message to channel)"
      (let [message ":zippy!zippy@72-13-84-243.somedomain.com PRIVMSG #ceptr :This is a dumb question but..."
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (is (re-find #"^zippy!zippy@72-13-84-243.somedomain.com-" (contents d :id)))
        (is (= (s-> key->resolve irc-contacts "zippy")  (contents d :from) ))
        (is (= (s-> key->resolve irc-contacts "#ceptr")  (contents d :to) ))
        (is (= :irc-stream  (contents d :channel) ))
        (is (= {:from "irc/from" :cmd "irc/command" :to "irc/channel" :message "text/plain"} (contents d :envelope)))
        (is (= {:from "zippy!zippy@72-13-84-243.somedomain.com" :cmd  "PRIVMSG"  :to "#ceptr" :message "This is a dumb question but..."} (contents d :content)))
        (is (= nil (handle-message b ":zippy!zippy@72-13-84-243.somedomain.com QUIT")))
        )
      )
    (testing "internal functions: handle-message (message to user)"
      (let [message ":zippy!zippy@72-13-84-243.somedomain.com PRIVMSG art :This is a dumb question but..."
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (is (re-find #"^zippy!zippy@72-13-84-243.somedomain.com-" (contents d :id)))
        (is (= (s-> key->resolve irc-contacts "zippy")  (contents d :from) ))
        (is (= (s-> key->resolve irc-contacts "art")  (contents d :to) ))
        (is (= :irc-stream  (contents d :channel) ))
        (is (= {:from "irc/from" :cmd "irc/command" :to "irc/user" :message "text/plain"} (contents d :envelope)))
        (is (= {:from "zippy!zippy@72-13-84-243.somedomain.com" :cmd  "PRIVMSG"  :to "art" :message "This is a dumb question but..."} (contents d :content)))
        )
      )
    
    ))
