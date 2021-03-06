(ns anansi.test.streamscapes.channels.socket-in
  (:use [anansi.streamscapes.channels.socket-in] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.receptor.host :only [host-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def]]
        [anansi.streamscapes.contact :only [contact-def]])
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest socket-in
  (let [m (make-receptor user-def nil "eric")
        h (make-receptor host-def nil {})
        r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor contact-def r {:attributes {:name "Eric"}})
        cc-addr (s-> matrice->make-channel r {:name :socket-stream})
        cc (get-receptor r cc-addr)
        b (make-receptor socket-in-def cc {})
        ip-contacts (get-scape r :ip-address-contact true)]
    (--> key->set r (get-scape r :channel-type) cc-addr :socket)
    (--> key->set b ip-contacts "127.0.0.0" (address-of eric))

    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    
    (testing "internal functions: handle-message"
      (let [message "some message"
            droplet-address (handle-message b {:from "192.168.1.1" :to "127.0.0.1" :message  message})
            d (get-receptor r droplet-address)
            ]
        (is (re-find #"^192.168.1.1-" (contents d :id)))
        (facts "about socket droplet"
          (contents d :from)  => (s-> key->resolve ip-contacts "192.168.1.1")
          (contents d :to) => (s-> key->resolve ip-contacts "127.0.0.1")
          (contents d :channel) => :socket-stream 
          (contents d :envelope) => {:from "ip/address" :message "text/plain"}
          (contents d :content) => {:from "192.168.1.1" :to "127.0.0.1" :message "some message"}
          (into [] (s-> key->resolve (get-scape r :droplet-grooves) droplet-address)) => [:simple-message]
          )
        )
      )    
    ))
