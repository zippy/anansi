(ns anansi.test.streamscapes.channels.local-bridge-out
  (:use [anansi.streamscapes.channels.local-bridge-out] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel :only [channel-def stream->send]]
        [anansi.streamscapes.channels.local-bridge-in]
        )
  (:use [clojure.test])
  (:use [midje.sweet])
  (:use [clj-time.core :only [now]]))

(deftest local-bridge-out
  (let [m (make-receptor user-def nil "eric")
        r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        c-out-addr (s-> matrice->make-channel r {:name :local-stream
                                                 :receptors {local-bridge-out-def {:role :deliverer :signal channel->deliver :params {}}}
                                                 })
        c-out (get-receptor r c-out-addr)
        eric-ss-addr (address-of r)
        u (make-receptor user-def nil "zippy")
        ru (make-receptor streamscapes-def nil {:matrice-addr (address-of u) :attributes {:_password "password" :data {:datax "x"}}})
        c-in-addr (s-> matrice->make-channel ru {:name :local-stream
                                                 :receptors {local-bridge-in-def {:role :receiver :signal cheat->receive :params {}}}
                                                 })
        zippy-ss-addr (address-of ru)
        [b-out-addr _] (s-> key->resolve (get-scape c-out :deliverer) :deliverer)
        b-out (get-receptor c-out b-out-addr)
        ]
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b-out true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))

    (testing "sending locally"
      (let [zippy-droplet-ids (get-scape ru :id)]
        (is (= (count (s-> key->all zippy-droplet-ids)) 0))
        (let [
              i-from (s-> matrice->identify r {:identifiers {:ss-address eric-ss-addr} :attributes {:name "Eric"}})
              i-to (s-> matrice->identify r {:identifiers {:ss-address zippy-ss-addr} :attributes {:name "Zippy"}})
              droplet-address (s-> matrice->incorporate r {:to i-to :from i-from :envelope {:subject "text/plain" :body "text/html"} :content {:subject "Hi there!" :body "<b>Hello world!</b>"}})
              result (s-> stream->send c-out {:droplet-address droplet-address })
              d (get-receptor r droplet-address)
              deliveries (get-scape r :delivery)
              ]

          (let [[time] (s-> address->resolve deliveries droplet-address)]
            (is (= result nil))
            (is (= (subs (str (now)) 0 19) (subs time 0 19))) ; hack off the milliseconds
            )
          (let [droplet-id (first (s-> address->all zippy-droplet-ids))
                zd-addr (first (s-> key->all zippy-droplet-ids))
                zd (get-receptor ru zd-addr)
                ss-addr-idents (get-scape ru :ss-address-ident)
                ]
            (is (= (count (s-> key->all zippy-droplet-ids)) 1))
            (is (= droplet-id (contents d :id)))
            (is (= (contents d :content) (contents zd :content)))
            (is (= (contents d :envelope) (contents zd :envelope)))
            (is (= (s-> key->resolve ss-addr-idents eric-ss-addr)  (contents zd :from) ))
            ))))))
