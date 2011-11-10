(ns anansi.test.streamscapes.channels.local-bridge-out
  (:use [anansi.streamscapes.channels.local-bridge-out] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.receptor.host :only [host-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def stream->send]]
        [anansi.streamscapes.channels.local-bridge-in]
        )
  (:use [clojure.test])
  (:use [midje.sweet])
  (:use [clj-time.core :only [now]]))

(deftest local-bridge-out
  (let [h (make-receptor host-def nil {})
        m (make-receptor user-def h "eric")
        r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        c-out-addr (s-> matrice->make-channel r {:name :local-stream
                                                 :receptors {local-bridge-out-def {:role :deliverer :signal ["anansi.streamscapes.channels.local-bridge-out" "channel" "deliver"] :params {}}}
                                                 })
        c-out (get-receptor r c-out-addr)
        eric-ss-addr (address-of r)
        u (make-receptor user-def h "zippy")
        ru (make-receptor streamscapes-def h {:matrice-addr (address-of u) :attributes {:_password "password" :data {:datax "x"}}})
        c-in-addr (s-> matrice->make-channel ru {:name :local-stream
                                                 :receptors {local-bridge-in-def {:role :receiver :signal ["anansi.streamscapes.channels.local-bridge-in" "cheat" "receive"] :params {}}}
                                                 })
        zippy-ss-addr (address-of ru)
        [b-out-addr _] (s-> key->resolve (get-scape c-out :deliverer) :deliverer)
        b-out (get-receptor c-out b-out-addr)
        ]
    (--> key->set r (get-scape r :channel-type) c-out-addr :streamscapes)
    (--> key->set ru (get-scape ru :channel-type) c-in-addr :streamscapes)
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b-out true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))

    (testing "sending locally"
      (let [zippy-droplet-ids (get-scape ru :id)]
        (is (= (count (s-> key->all zippy-droplet-ids)) 0))
        (let [
              i-from (s-> matrice->identify r {:identifiers {:streamscapes-address eric-ss-addr} :attributes {:name "Eric"}})
              i-to (s-> matrice->identify r {:identifiers {:streamscapes-address zippy-ss-addr} :attributes {:name "Zippy"}})
              receiver-contact-addr (s-> matrice->identify ru {:identifiers {:streamscapes-address zippy-ss-addr} :attributes {:name "Zippy"}})
              ru-contact-names (get-scape ru :contact-name)
              droplet-address (s-> matrice->incorporate r {:to i-to :from i-from :envelope {:subject "text/plain" :body "text/html"} :content {:subject "Hi there!" :body "<b>Hello world!</b>"}})
              ru-contact-name-before-send (s-> key->resolve ru-contact-names receiver-contact-addr)
              result (s-> stream->send c-out {:droplet-address droplet-address })
              d (get-receptor r droplet-address)
              deliveries (get-scape r :delivery)
              ]

          (let [[time] (s-> address->resolve deliveries droplet-address)]
            (is (= result nil))
            (is (= (subs (str (now)) 0 19) (subs time 0 19))) ; hack off the milliseconds
            
            (let [droplet-id (first (s-> address->all zippy-droplet-ids))
                  zd-addr (first (s-> key->all zippy-droplet-ids))
                  zd (get-receptor ru zd-addr)
                  ss-addr-contacts (get-scape ru :streamscapes-address-contact)
                  ]
              (is (= (count (s-> key->all zippy-droplet-ids)) 1))
              (is (= droplet-id (contents d :id)))
              (is (= (contents d :content) (contents zd :content)))
              (is (= (contents d :envelope) (contents zd :envelope)))
              (is (= (s-> key->resolve ss-addr-contacts eric-ss-addr)  (contents zd :from) ))
              (facts "about auto identifying on receive not overwriting existing contact names"
                (let [zipster (get-receptor ru receiver-contact-addr)]
                  (s-> key->resolve ss-addr-contacts zippy-ss-addr) => (contents zd :to)
                  (contents zipster :name) => "Zippy"
                  ru-contact-name-before-send => "Zippy"
                  (s-> key->resolve ru-contact-names receiver-contact-addr) => "Zippy")
                  )
              )
            
            (facts "about using deliver flag when incororating a droplet"
              (let [droplet-address2 (s-> matrice->incorporate r {:deliver :immediate :channel :local-stream :to i-to :from i-from :envelope {:subject "text/plain" :body "text/html"} :content {:subject "Another Droplet" :body "<b>Hello again world!</b>"}})]
                (count (s-> key->all zippy-droplet-ids)) => 2
                (= droplet-address droplet-address2) => false
                (let [[time2] (s-> address->resolve deliveries droplet-address2)]
                  (is (= (subs (str (now)) 0 19) (subs time2 0 19))) ; hack off the milliseconds
                  (= time time2) => false))
              (s-> matrice->incorporate r {:deliver :immediate :channel :fish}) => (throws RuntimeException "Unknown channel: :fish")
              ))
          )
        
    ))

    ))
