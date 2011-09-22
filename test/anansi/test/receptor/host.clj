(ns anansi.test.receptor.host
  (:use [anansi.receptor.host] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user])
  (:use [midje.sweet])
  (:use [clojure.test]))

(let [h (receptor :host nil)
      ha (address-of h)]
  (facts "scaping relationships"
    (scape-relationship (get-scape h :room) :key) => :name
    (scape-relationship (get-scape h :room) :address) => :address
    (scape-relationship (get-scape h :user) :key) => :name
    (scape-relationship (get-scape h :user) :address) => :address
    (scape-relationship (get-scape h :stream) :key) => :name
    (scape-relationship (get-scape h :stream) :address) => :address
    (scape-relationship (get-scape h :session) :key) => :sha
    (scape-relationship (get-scape h :session) :address) => :user-addr-time-interface-map)
  (deftest host
    (testing "ping"
      (is (re-find #"Hi [0-9]+! This is the host." (s-> ceptr->ping h nil)))
      )
    (testing "ceptr->host-room"
      (let [addr (s-> self->host-room h {:name "the room" :password "pass" :matrice-address 1 :data {:background-url "http://someure.com/pic.jpg"}})
            r (get-receptor h addr)]
        (is (= addr (s-> key->resolve (get-scape h :room) "the room")))
        (is (= (contents r :password) "pass") )
        (is (= (contents r :data) {:background-url "http://someure.com/pic.jpg"}) )
        (is (= (s-> key->all (get-scape r :matrice)) [1]))))

    (testing "self->host-streamscape"
      (let [addr (s-> self->host-streamscape h {:name "erics-streamscape" :password "pass" :matrice-address 1 :data {}})
            r (get-receptor h addr)]
        (is (= addr (s-> key->resolve (get-scape h :stream) "erics-streamscape")))
        (is (= (contents r :password) "pass") )
        (is (= (contents r :data) {}) )
        (is (= (s-> key->all (get-scape r :matrice)) [1]) )))
    
    (testing "self->host-user"
      (let [addr (s-> self->host-user h "zippy")
            u (get-receptor h addr)]
        (is (= ["zippy"] (s-> key->all (get-scape h :user))))
        (is (= addr (s-> self->host-user h "zippy")))
        (is (= (contents u :name) "zippy"))))

    (testing "command->send-signal"
      (let [_ (s-> key->set (get-scape h :session) "1234" {:user (resolve-name h "zippy")})
            result (s-> command->send-signal h {:prefix "receptor.host" :aspect "ceptr" :signal "ping" :session "1234" :to 0 :params nil})
            ]
        (is (= result "Hi 8! This is the host."))
        (is (thrown-with-msg? RuntimeException #"Unknown signal: receptor.host.ceptr->pong"
              (s-> command->send-signal h {:signal "pong" :aspect "ceptr" :prefix "receptor.host" :session "1234" :to 0 :params nil}))
            )
        (is (thrown-with-msg? RuntimeException #"Unknown session: 12345"
              (s-> command->send-signal h {:signal "ping" :aspect "ceptr" :prefix "receptor.host" :session "12345" :to 0 :params nil}))
            )))
 
    (testing "command->authenticate"
      (let [i (receptor :some-interface h)
            s (--> command->authenticate i h {:user "zippy"})
            sessions (get-scape h :session)
            {user-addr :user time :time interface :interface} (s-> key->resolve sessions s)
            ]
        (is (= user-addr (resolve-name h "zippy")))
        (is (= interface :some-interface))
        (is (thrown-with-msg? RuntimeException #"authentication failed for user: squid"
              (--> command->authenticate i h {:user "squid"})))
        ))
    (testing "command->new-user"
      (let [i (receptor :some-interface h)]
        (is (thrown-with-msg? RuntimeException #"username 'zippy' in use"
              (--> command->new-user i h {:user "zippy"})))
        (is (= (--> command->new-user i h {:user "eric"}) (resolve-name h "eric")))
        ))
    (testing "command->get-state"
      (let [i (receptor :some-interface h)
            addr (resolve-name h "eric")]
        (is (thrown-with-msg? RuntimeException #"unknown receptor: 0"
              (--> command->get-state i h {:receptor "0"})))
        (is (= {:name "eric", :type :user, :address addr, :changes 0} (--> command->get-state i h {:receptor addr}) ))
        ))
    (testing "restore"
      (is (=  (state h true) (state (restore (state h true) nil) true))))
    ))
