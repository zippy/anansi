(ns anansi.test.streamscapes.channels.twitter-bridge-in
  (:use [anansi.streamscapes.channels.twitter-bridge-in] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.receptor.host :only [host-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def]]
        [anansi.streamscapes.ident :only [ident-def]])
  (:use [midje.sweet])
  (:use [clojure.test]))

(deftest twitter-bridge-in
  (let [h (make-receptor host-def nil {})
        m (make-receptor user-def h "eric")
        r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor ident-def r {:attributes {:name "Eric"}})
        twp (make-receptor ident-def r {:attributes {:name "Twitter Public"}})
        cc-addr (s-> matrice->make-channel r {:name :twitter-stream})
        cc (get-receptor r cc-addr)
        b (make-receptor twitter-bridge-in-def cc {})
        twitter-idents (get-scape r :twitter-ident true)]
    (--> key->set r (get-scape r :channel-type) cc-addr :twitter)
    (--> key->set b twitter-idents "@zippy314" (address-of eric))
    (--> key->set b twitter-idents "_twp_" (address-of twp))

    (fact
      (receptor-state b false) => (contains {:fingerprint :anansi.streamscapes.channels.twitter-bridge-in.twitter-bridge-in}))
    
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    
    (testing "internal functions: handle-message (message to channel)"
      (let [message {:id_str "121470088258916352" :text "Some short tweet" :from_user "zippy314" :profile_image_url "http://someurl" :created_at "Wed Oct 04 09:21:40 +0000 2011"}
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            deliveries (get-scape r :delivery)
            avatars (get-scape r :ident-twitter-avatar)
            ]
        (facts "about twitter droplet"
          (contents d :id) => "121470088258916352"
          (contents d :from) => (s-> key->resolve twitter-idents "@zippy314")
          (s-> key->resolve twitter-idents "@zippy314")
          (s-> key->resolve avatars (contents d :from)) => "http://someurl"
          (contents d :to) => (s-> key->resolve twitter-idents "_twp_")
          (contents d :envelope) => {:from "twitter/screen_name" :text "text/plain"}
          (contents d :content) => {:from "zippy314" :text "Some short tweet"}
          (s-> key->resolve (get-scape r :droplet-grooves) droplet-address) => [:simple-message]
          (let [[time] (s-> address->resolve deliveries droplet-address)]
            (fact "2011-10-04T03:21:40.000Z" => time)
            )
          )
        (is (= :twitter-stream  (contents d :channel) ))
        (is (= droplet-address (handle-message b message)))))
    (facts "about content groove scaping (punkmoney)"
      (let [message {:id_str "12147008825891" :text "@artbrock I promise to pay, on demand, some squids. Expires in 1 year. #punkmoney" :from_user "zippy314" :profile_image_url "http://someurl" :created_at "Wed Oct 04 09:31:40 +0000 2011"}
            droplet-address (handle-message b message)
            d (get-receptor r droplet-address)
            ]
        (s-> key->resolve (get-scape r :droplet-grooves) droplet-address) => [:punkmoney :simple-message]
        )
      )
    ))
