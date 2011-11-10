(ns anansi.test.streamscapes.channels.xmpp-controller
  (:use [anansi.streamscapes.channels.xmpp-controller] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.receptor.host :only [host-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.contact :only [contact-def]]
        [anansi.streamscapes.channel]
        [anansi.streamscapes.channels.xmpp-bridge-out :only [xmpp-bridge-out-def]]
        [anansi.streamscapes.channels.xmpp-bridge-in :only [xmpp-bridge-in-def controller->receive]]
        )
  (:use [midje.sweet])
  (:use [clojure.test]))

(facts "about the xmpp controller"
  (let [m (make-receptor user-def nil "eric")
        h (make-receptor host-def nil {})
        r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor contact-def r {:attributes {:name "Eric"}})
        eric-contact-addr (address-of eric)
        zippy (make-receptor contact-def r {:attributes {:name "Zippy"}})
        
        channel-address (s-> matrice->make-channel r {:name :xmpp-stream
                                                      :receptors {xmpp-bridge-in-def {:role :receiver :params {} }
                                                                  xmpp-bridge-out-def {:role :deliverer :signal ["anansi.streamscapes.channels.xmpp-bridge-out" "channel" "deliver"] :params {}}
                                                                  xmpp-controller-def {:role :controller :signal ["anansi.streamscapes.channels.xmpp-controller" "channel" "control"] :params {:attributes {:host "jabber.org" :username "zippy314@jabber.org" :domain "jabber.org" :_password "somepass"}}}}
                                                          })
        cc (get-receptor r channel-address)
        [controller-address control-signal] (get-controller cc)
        [receiver-address _] (get-receiver-bridge cc)
        receiver (get-receptor cc receiver-address)
        b (get-receptor cc controller-address)
        xmpp-contacts (get-scape r :xmpp-address-contact true)]
    (--> key->set r (get-scape r :channel-type) channel-address :xmpp)
    (--> key->set b xmpp-contacts "zippy.314.ehb@gmail.com" eric-contact-addr)
;;    (--> key->set b xmpp-contacts "zippy314@jabber.org" (address-of zippy))
    (--> key->resolve b xmpp-contacts "zippy314@jabber.org") =not=> nil
    (receptor-state b false) => (contains {:fingerprint :anansi.streamscapes.channels.xmpp-controller.xmpp-controller
                                           :username "zippy314@jabber.org"
                                           :domain "jabber.org"
                                           :host "jabber.org"
                                           })
    (receptor-state b false) =not=> (contains {:_password "somepass"})
    
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    (facts "about contents"
      (contents b :username) => "zippy314@jabber.org"
      (contents b :domain) => "jabber.org"
      (contents b :host) => "jabber.org"
      (contents b :_password) => "somepass")
    
    (s-> channel->control b {:command :fish}) => (throws RuntimeException "Unknown control command: :fish" )

    (facts "about logging into xmpp server and sending a message"
      (let [droplet-ids (get-scape r :id)]
        (count (s-> key->all droplet-ids)) => 0
        
        (s-> channel->control b {:command :status}) => :closed
        (if (= (contents b :_password) "somepass")
          ;; test without valid password       
          (do
            (s-> channel->control b {:command :open}) => (throws RuntimeException "SASL authentication failed using mechanism DIGEST-MD5:")
            (s-> channel->control b {:command :close}) => (throws RuntimeException "Channel not open")
            )
          ;; test with valid password
          (do
            (s-> channel->control b {:command :open})
            (s-> channel->control b {:command :status}) => :open
            (s-> matrice->incorporate r {:deliver :immediate :channel :xmpp-stream :to eric-contact-addr :envelope {:from "address/xmpp" :to "address/xmpp" :body "text/plain" :subject "text/plain" :thread "thread/xmpp" :error "error/xmpp" :type "message-type/xmpp"}  :content {:body "This is a test message."}})
            ;;(Thread/sleep 10000)
            (s-> channel->control b {:command :close})
            (s-> channel->control b {:command :status}) => :closed
            )
          )
        
        (let [d (s-> controller->receive receiver {:body "boinkers", :subject nil, :thread nil, :from "zippy.314.ehb@gmail.com", :to "zippy314@jabber.org", :packet-id "BF64C760B1917AD8_6", :error nil, :type :chat})
              s (receptor-state (get-receptor r d) false)]
          s => (contains {:from eric-contact-addr :matched-grooves {:simple-message {:message "boinkers"}}})
          s => (contains {:content {:body "boinkers", :subject nil, :thread nil, :from "zippy.314.ehb@gmail.com", :to "zippy314@jabber.org", :packet-id "BF64C760B1917AD8_6", :error nil, :type :chat}})
          )
        ))))
