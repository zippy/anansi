(ns anansi.test.streamscapes.channels.xmpp-bridge-in
  (:use [anansi.streamscapes.channels.xmpp-bridge-in] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.receptor.host :only [host-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def]]
        [anansi.streamscapes.contact :only [contact-def]])
  (:use [midje.sweet])
  (:use [clojure.test]))

(facts "about xmpp bridge in"
  (let [m (make-receptor user-def nil "eric")
        h (make-receptor host-def nil {})
        r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor contact-def r {:attributes {:name "Eric"}})
        art (make-receptor contact-def r {:attributes {:name "Art"}})
       ;; equiv of irc channel?  gmail (make-receptor contact-def r {:attributes {:name "ceptr-channel"}})
        cc-addr (s-> matrice->make-channel r {:name :xmpp-stream})
        cc (get-receptor r cc-addr)
        b (make-receptor xmpp-bridge-in-def cc {})
        xmpp-contacts (get-scape r :xmpp-address-contact true)]
    (--> key->set r (get-scape r :channel-type) cc-addr :xmpp)
    (--> key->set b xmpp-contacts "zippy314@streamscapes.com" (address-of eric))
    (--> key->set b xmpp-contacts "art@streamscapes.com" (address-of art))
    ;(--> key->set b xmpp-contacts "#ceptr" (address-of ceptr-channel))

    (receptor-state b false) => (contains {:fingerprint :anansi.streamscapes.channels.xmpp-bridge-in.xmpp-bridge-in})
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))

    (facts "about receiving a simple chat message"
      (let [message {:body "This is a dumb question but..."
                     :subject nil
                     :thread ""
                     :from "zippy314@streamscapes.com"
                     :to "art@streamscapes.com"
                     :packet-id 1234
                     :error {}
                     :type :chat}
            
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (contents d :id) => 1234
        (contents d :from) => (address-of eric)
        (contents d :to) => (address-of art)
        (contents d :channel) => :xmpp-stream
        (contents d :envelope) => {:from "address/xmpp" :to "address/xmpp" :body "text/plain" :subject "text/plain" :thread "thread/xmpp" :error "error/xmpp" :type "message-type/xmpp"}
        (contents d :content) => {:body "This is a dumb question but..."
                                  :subject nil
                                  :thread ""
                                  :from "zippy314@streamscapes.com"
                                  :to "art@streamscapes.com"
                                  :packet-id 1234
                                  :error {}
                                  :type :chat}
        (s-> key->resolve (get-scape r :droplet-grooves) droplet-address) => [:simple-message]
        (handle-message b message) => (address-of d)
        ))))

