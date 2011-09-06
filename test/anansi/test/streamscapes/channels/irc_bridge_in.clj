(ns anansi.test.streamscapes.channels.irc-bridge-in
  (:use [anansi.streamscapes.channels.irc-bridge-in] :reload)
  (:use [anansi.streamscapes.channel])
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.streamscapes.streamscapes])
  (:use [clojure.test]))

(deftest irc-bridge-in
  (let [m (receptor :user nil "eric" nil)
        r (receptor :streamscapes nil (address-of m) "password" {:datax "x"})
        eric (receptor :ident r {:name "Eric"})
        art (receptor :ident r {:name "Art"})
        ceptr-channel (receptor :ident r {:name "ceptr-channel"})
        cc (receptor :channel r :irc-stream)
        b (receptor :irc-bridge-in cc {})
        irc-idents (get-scape r :irc-ident true)]
    (--> key->set b irc-idents "zippy" (address-of eric))
    (--> key->set b irc-idents "art" (address-of art))
    (--> key->set b irc-idents "#ceptr" (address-of ceptr-channel))
    ;    (testing "contents" )
    (testing "restore"
      (is (=  (state cc true) (state (restore (state cc true) nil) true))))
    
    (testing "internal functions: handle-message (message to channel)"
      (let [message ":zippy!zippy@72-13-84-243.somedomain.com PRIVMSG #ceptr :This is a dumb question but..."
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (is (re-find #"^zippy!zippy@72-13-84-243.somedomain.com-" (contents d :id)))
        (is (= (s-> key->resolve irc-idents "zippy")  (contents d :from) ))
        (is (= (s-> key->resolve irc-idents "#ceptr")  (contents d :to) ))
        (is (= :irc-stream  (contents d :aspect) ))
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
        (is (= (s-> key->resolve irc-idents "zippy")  (contents d :from) ))
        (is (= (s-> key->resolve irc-idents "art")  (contents d :to) ))
        (is (= :irc-stream  (contents d :aspect) ))
        (is (= {:from "irc/from" :cmd "irc/command" :to "irc/user" :message "text/plain"} (contents d :envelope)))
        (is (= {:from "zippy!zippy@72-13-84-243.somedomain.com" :cmd  "PRIVMSG"  :to "art" :message "This is a dumb question but..."} (contents d :content)))
        )
      )
    
    ))
