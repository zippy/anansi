(ns anansi.test.streamscapes.bridge-local-in
  (:use [anansi.streamscapes.bridge-local-in] :reload)
  (:use [anansi.streamscapes.channel])
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.streamscapes.streamscapes])
  (:use [clojure.test]))


(deftest bridge-local-in
  (let [m (receptor :user nil "eric" nil)
        r (receptor :streamscapes nil (address-of m) "password" {:datax "x"})
        eric-addr (s-> matrice->identify r {:identifiers {:ss-address (address-of r)} :attributes {:name "Eric"}})
        eric-ss-addr (address-of r)
        u (receptor :user nil "zippy" nil)
        ru (receptor :streamscapes nil (address-of u) "password" {:datax "x"})
        zippy-ss-addr (address-of ru)
        zippy-addr (s-> matrice->identify r {:identifiers {:ss-address (address-of ru)} :attributes {:name "Zippy"}})
        cc (receptor :channel r :local-stream)
        b (receptor :bridge-local-in cc {})
        ss-addr-idents (get-scape r :ss-address-ident)]
    
    (testing "restore"
      (is (=  (state b true) (state (restore (state b true) nil) true))))
    (testing "internal functions: handle-message"
      (let [message {:id "1.2" :to eric-ss-addr :from zippy-ss-addr :envelope {:subject "text/plain" :body "text/html"} :content {:subject "Hi there!" :body "<b>Hello world!</b>"}}
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (is (= "1.2" (contents d :id)))
        (is (= eric-addr (contents d :to)))
        (is (= zippy-addr (contents d :from) ))
        (is (= (s-> key->resolve ss-addr-idents zippy-ss-addr)  (contents d :from) ))
        (is (= :local-stream  (contents d :aspect) ))
        (is (= {:subject "text/plain" :body "text/html"} (contents d :envelope)))
        (is (= {:subject "Hi there!" :body "<b>Hello world!</b>"} (contents d :content)))
        (is (= droplet-address (handle-message b message)))
        )
      )
    ))
