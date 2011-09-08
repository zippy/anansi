(ns anansi.test.streamscapes.channels.socket-in
  (:use [anansi.streamscapes.channels.socket-in] :reload)
  (:use [anansi.streamscapes.channel])
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.streamscapes.streamscapes])
  (:use [clojure.test]))

(deftest socket-in
  (let [m (receptor :user nil "eric" nil)
        r (receptor :streamscapes nil (address-of m) "password" {:datax "x"})
        eric (receptor :ident r {:name "Eric"})
        cc (receptor :channel r :socket-stream)
        b (receptor :socket-in cc {})
        ip-idents (get-scape r :ip-ident true)]
    (--> key->set b ip-idents "127.0.0.0" (address-of eric))
    ;    (testing "contents" )
    (testing "restore"
      (is (=  (state cc true) (state (restore (state cc true) nil) true))))
    
    (testing "internal functions: handle-message"
      (let [message "some message"
            droplet-address (handle-message b {:from "192.168.1.1" :to "127.0.0.1" :message  message})
            d (get-receptor r droplet-address)
            ]
        (is (re-find #"^192.168.1.1-" (contents d :id)))
        (is (= (s-> key->resolve ip-idents "192.168.1.1")  (contents d :from) ))
        (is (= (s-> key->resolve ip-idents "127.0.0.1")  (contents d :to) ))
        (is (= :socket-stream  (contents d :aspect) ))
        (is (= {:from "ip/address" :message "text/plain"} (contents d :envelope)))
        (is (= {:from "192.168.1.1" :to "127.0.0.1" :message "some message"} (contents d :content)))
        )
      )    
    ))
