(ns anansi.test.streamscapes.channels.socket-controller
  (:use [anansi.streamscapes.channels.socket-controller] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.receptor.host :only [host-def]]
        [anansi.streamscapes.channel]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.contact :only [contact-def]]
        [anansi.streamscapes.channels.socket-in :only [socket-in-def]]
        [anansi.test.helpers :only [write connect]])
  (:use [midje.sweet])
  (:use [clojure.test])
  )

(defn test-input-function [input ip] (println (str "from: " ip " processed: " input)))
(deftest socket-controller
  (let [m (make-receptor user-def nil "eric")
        h (make-receptor host-def nil {})
        r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        eric (make-receptor contact-def r {:attributes {:name "Eric"}})
        house (make-receptor contact-def r {:attributes {:name "my-house"}})
        channel-address (s-> matrice->make-channel r
                             {:name :socket-stream
                              :receptors {socket-in-def {:role :receiver :signal ["anansi.streamscapes.channels.socket-in" "controller" "receive"] :params {}}
                                          socket-controller-def {:role :controller :signal ["anansi.streamscapes.channels.socket-controller" "channel" "control"] :params {:attributes {:port 3141 :input-function test-input-function}}}}
                                                          })
        cc (get-receptor r channel-address)
        [controller-address control-signal] (get-controller cc)
        b (get-receptor cc controller-address)
        ip-contacts (get-scape r :ip-address-contact true)
        droplet-ids (get-scape r :id)]
    (--> key->set r (get-scape r :channel-type) channel-address :socket)
    (--> key->set b ip-contacts "127.0.0.1" (address-of eric))
    
    (fact
      (receptor-state b false) => (contains {:port 3141 :input-function fn?}))
    (testing "contents"
      (is (= 3141 (contents b :port))))
    (facts "about restoring serialized receptor"
      (let [state (receptor-state b true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))
    (testing "listening on a socket and receiving a signal on it"
      (is (= (s-> channel->control b {:command :status}) :closed))
      (s-> channel->control b {:command :open})
      (is (= (s-> channel->control b {:command :status}) :open))
      (is (= (count (s-> key->all droplet-ids)) 0))
      (let [conn (connect "127.0.0.1" 3141)]
        (write conn "test message 1")
        (write conn "test message 2")
        )

      (while (< (count (s-> key->all droplet-ids)) 1) "idling")
      
      (let [droplet-id (first (s-> address->all droplet-ids))
            d-addr (first (s-> key->all droplet-ids))
            d (get-receptor r d-addr)
            content (contents d :content)]
        
        (is (= droplet-id (contents d :id)))
        (is (= "test message 1" (:message content)))
        (is (= "127.0.0.1" (:from content)))
        (is (= (s-> key->resolve ip-contacts "127.0.0.1")  (contents d :from) ))
        )
      
      (s-> channel->control b {:command :close})
      (is (= (s-> channel->control b {:command :status}) :closed)))
    (testing "unknown control signal"
      (is (thrown-with-msg? RuntimeException #"Unknown control command: :fish" (s-> channel->control b {:command :fish})))
      )))
