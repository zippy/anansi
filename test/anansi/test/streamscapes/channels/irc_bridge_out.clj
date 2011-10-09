(ns anansi.test.streamscapes.channels.irc-bridge-out
  (:use [anansi.streamscapes.channels.irc-bridge-out] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.ident :only [ident-def]]
        [anansi.streamscapes.channel :only [channel-def]])
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest irc-bridge-out
  (let [m (make-receptor user-def nil "eric")
        r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor ident-def r {:attributes {:name "Eric"}})
        cc-addr (s-> matrice->make-channel r {:name :irc-stream})
        cc (get-receptor r cc-addr)
        b (make-receptor irc-bridge-out-def cc {})
        irc-idents (get-scape r :irc-ident true)]
    (--> key->set b irc-idents "zippy" (address-of eric))
    
    (fact
      (receptor-state b false) => (contains {:fingerprint :anansi.streamscapes.channels.irc-bridge-out.irc-bridge-out}))
    
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    ))
