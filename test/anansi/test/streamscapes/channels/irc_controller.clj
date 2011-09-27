(ns anansi.test.streamscapes.channels.irc-controller
  (:use [anansi.streamscapes.channels.irc-controller] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
                [anansi.receptor.user :only [user-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.ident :only [ident-def]]
        [anansi.streamscapes.channel]
        [anansi.streamscapes.channels.irc-bridge-out :only [irc-bridge-out-def channel->deliver]]
        [anansi.streamscapes.channels.irc-bridge-in :only [irc-bridge-in-def]]
        )
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest irc-controller
  (let [m (make-receptor user-def nil "eric")
        r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor ident-def r {:attributes {:name "Eric"}})
        ceptr-channel (make-receptor ident-def r {:attributes {:name "ceptr channel"}})
        channel-address (s-> matrice->make-channel r {:name :irc-stream
                                                      :receptors {irc-bridge-in-def {:role :receiver :params {} }
                                                                  irc-bridge-out-def {:role :deliverer :signal channel->deliver :params {}}
                                                                  irc-controller-def {:role :controller :signal channel->control :params {:attributes {:host "irc.freenode.net" :port 6667 :user "Eric" :nick "zippy31415"}}}}
                                                          })
        cc (get-receptor r channel-address)
        [controller-address control-signal] (get-controller cc)
        b (get-receptor cc controller-address)
        irc-idents (get-scape r :irc-ident true)]
    (--> key->set b irc-idents "zippy31415" (address-of eric))
    (--> key->set b irc-idents "#ceptr" (address-of ceptr-channel))
    
    (fact
      (receptor-state b false) => (contains {:fingerprint :anansi.streamscapes.channels.irc-controller.irc-controller
                                             :user "Eric"
                                             :nick "zippy31415"
                                             :host "irc.freenode.net"
                                             :port 6667}))
    
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    (testing "contents"
      (is (= "Eric" (contents b :user)))
      (is (= "zippy31415" (contents b :nick)))
      (is (= "irc.freenode.net" (contents b :host)))
      (is (= 6667 (contents b :port))))
    (comment ;THIS TESTING CODE IS DISABLED BECAUSE I DON"T WANT TO
             ;RUN IT ALL THE TIME.
     testing "logging into irc server, joining a channel and sending a message"
      (is (= (s-> channel->control b {:command :status}) :closed))
      (s-> channel->control b {:command :open})
      (is (= (s-> channel->control b {:command :status}) :open))
      (s-> channel->control b {:command :join :params {:channel "#ceptr"}})
      (Thread/sleep 13000)
      (let [droplet-ids (get-scape r :id)
            droplet-address (s-> matrice->incorporate r {:to (address-of ceptr-channel) :envelope {:message "text/plain"} :content {:message "This is a test message."}})
            sent-d (get-receptor r droplet-address)
            result (s-> stream->send cc {:droplet-address droplet-address })]
        (is (= (count (s-> key->all droplet-ids)) 1))
        
      
        (Thread/sleep 13000)

        (let [x 1]
          (is (= (count (s-> key->all droplet-ids)) 1))
                                        ;            (is (= droplet-id (contents d :id)))
                                        ;            (is (= (contents d :content) (contents zd :content)))
                                        ;            (is (= (contents d :envelope) (contents zd :envelope)))
                                        ;            (is (= (s-> key->resolve ss-addr-idents eric-ss-addr)  (contents zd :from) ))
          ))
      
      (s-> channel->control b {:command :close})
      (is (= (s-> channel->control b {:command :status}) :closed))
      )
    (testing "unknown control signal"
      (is (thrown-with-msg? RuntimeException #"Unknown control command: :fish" (s-> channel->control b {:command :fish})))
      )))
