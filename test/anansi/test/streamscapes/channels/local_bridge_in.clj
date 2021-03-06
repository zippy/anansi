(ns anansi.test.streamscapes.channels.local-bridge-in
  (:use [anansi.streamscapes.channels.local-bridge-in] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.receptor.host :only [host-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def]])
  (:use [midje.sweet])
  (:use [clojure.test]))


(deftest local-bridge-in
  (let [m (make-receptor user-def nil "eric")
        h (make-receptor host-def nil {})
        r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric-addr (s-> matrice->identify r {:identifiers {:streamscapes-address (address-of r)} :attributes {:name "Eric"}})
        eric-ss-addr (address-of r)
        u (make-receptor user-def nil "zippy")
        ru (make-receptor streamscapes-def nil {:matrice-addr (address-of u) :attributes {:_password "password" :data {:datax "x"}}})
        zippy-ss-addr (address-of ru)
        zippy-addr (s-> matrice->identify r {:identifiers {:streamscapes-address (address-of ru)} :attributes {:name "Zippy"}})
        cc-addr (s-> matrice->make-channel r {:name :local-stream})
        cc (get-receptor r cc-addr)
        b (make-receptor local-bridge-in-def cc {})
        ss-addr-contacts (get-scape r :streamscapes-address-contact)]
    (--> key->set r (get-scape r :channel-type) cc-addr :streamscapes)
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    (testing "internal functions: handle-message"
      (let [message {:id "1.2" :to eric-ss-addr :from zippy-ss-addr :envelope {:subject "text/plain" :body "text/html"} :content {:subject "Hi there!" :body "<b>Hello world!</b>"}}
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (fact (s-> key->resolve (get-scape r :subject-body-message-groove) droplet-address) => true)
        (is (= "1.2" (contents d :id)))
        (is (= eric-addr (contents d :to)))
        (is (= zippy-addr (contents d :from) ))
        (is (= (s-> key->resolve ss-addr-contacts zippy-ss-addr)  (contents d :from) ))
        (is (= :local-stream  (contents d :channel) ))
        (is (= {:subject "text/plain" :body "text/html"} (contents d :envelope)))
        (is (= {:subject "Hi there!" :body "<b>Hello world!</b>"} (contents d :content)))
        (is (= droplet-address (handle-message b message)))
        (let [eric (get-receptor r eric-addr)]
          (fact (contents eric :name) => "Eric"))
        )
      )
    ))
