(ns anansi.test.streamscapes.channels.socket-in
  (:use [anansi.streamscapes.channels.socket-in] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def]]
        [anansi.streamscapes.ident :only [ident-def]])
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest socket-in
  (let [m (make-receptor user-def nil "eric")
        r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor ident-def r {:attributes {:name "Eric"}})
        cc-addr (s-> matrice->make-channel r {:name :socket-stream})
        cc (get-receptor r cc-addr)
        b (make-receptor socket-in-def cc {})
        ip-idents (get-scape r :ip-ident true)]
    (--> key->set b ip-idents "127.0.0.0" (address-of eric))

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
        (is (= (s-> key->resolve ip-idents "192.168.1.1")  (contents d :from) ))
        (is (= (s-> key->resolve ip-idents "127.0.0.1")  (contents d :to) ))
        (is (= :socket-stream  (contents d :channel) ))
        (is (= {:from "ip/address" :message "text/plain"} (contents d :envelope)))
        (is (= {:from "192.168.1.1" :to "127.0.0.1" :message "some message"} (contents d :content)))
        )
      )    
    ))
