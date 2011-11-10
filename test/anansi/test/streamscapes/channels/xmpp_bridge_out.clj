(ns anansi.test.streamscapes.channels.xmpp-bridge-out
  (:use [anansi.streamscapes.channels.xmpp-bridge-out] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.contact :only [contact-def]]
        [anansi.streamscapes.channel :only [channel-def]])
  (:use [midje.sweet])
  (:use [clojure.test]))

(facts "about xmpp-bridge-out"
  (let [m (make-receptor user-def nil "eric")
        r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor contact-def r {:attributes {:name "Eric"}})
        cc-addr (s-> matrice->make-channel r {:name :xmpp-stream})
        cc (get-receptor r cc-addr)
        b (make-receptor xmpp-bridge-out-def cc {})
        xmpp-contacts (get-scape r :xmpp-address-contact true)]
    (--> key->set b xmpp-contacts "zippy" (address-of eric))
    
    (receptor-state b false) => (contains {:fingerprint :anansi.streamscapes.channels.xmpp-bridge-out.xmpp-bridge-out})
    
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    ))
