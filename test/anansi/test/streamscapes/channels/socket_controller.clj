(ns anansi.test.streamscapes.channels.socket-controller
  (:use [anansi.streamscapes.channels.socket-controller] :reload)
  (:use [anansi.streamscapes.channel])
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channels.socket-in :only [controller->receive]]
        [anansi.test.helpers :only [write connect]])
  (:use [clojure.test])
  )

(deftest socket-controller
  (let [m (receptor :user nil "eric" nil)
        r (receptor :streamscapes nil (address-of m) "password" {:datax "x"})
        eric (receptor :ident r {:name "Eric"})
        house (receptor :ident r {:name "my-house"})
        channel-address (s-> matrice->make-channel r
                             {:name :socket-stream
                              :receptors {:local-bridge-in {:role :receiver :signal controller->receive :params {}}
                                          :socket-controller {:role :controller :signal channel->control :params {:port 3141 :input-function (fn [input ip] (println (str "from: " ip " processed: " input)))}}}
                                                          })
        cc (get-receptor r channel-address)
        [controller-address control-signal] (get-controller cc)
        b (get-receptor cc controller-address)
        ip-idents (get-scape r :ip-ident true)
        droplet-ids (get-scape r :id)]
    (--> key->set b ip-idents "127.0.0.1" (address-of eric))

    (testing "contents"
      (is (= 3141 (contents b :port))))
    (testing "restore"
      (is (=  (state cc true) (state (restore (state cc true) nil) true))))
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
        (is (= (s-> key->resolve ip-idents "127.0.0.1")  (contents d :from) ))
        )
      
      (s-> channel->control b {:command :close})
      (is (= (s-> channel->control b {:command :status}) :closed)))
    (testing "unknown control signal"
      (is (thrown-with-msg? RuntimeException #"Unknown control command: :fish" (s-> channel->control b {:command :fish})))
      )))
