(ns anansi.test.receptor.host
  (:use [anansi.receptor.host] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user])
  (:use [clojure.test]))

(deftest host
  (let [h (receptor :host nil)
        ha (address-of h)]
    (testing "host-room"
      (let [addr (s-> self->host-room h {:name "the room" :password "pass" :matrice-address 1 :data {:background-url "http://someure.com/pic.jpg"}})
            r (get-receptor h addr)]
        (is (= addr (s-> key->resolve (get-scape h :room) "the room")))
        (is (= (contents r :password) "pass") )
        (is (= (contents r :data) {:background-url "http://someure.com/pic.jpg"}) )
        (is (= (s-> key->all (get-scape r :matrice)) [1]))))

    (testing "host-streamscape"
      (let [addr (s-> self->host-streamscape h {:name "erics-streamscape" :password "pass" :matrice-address 1 :data {}})
            r (get-receptor h addr)]
        (is (= addr (s-> key->resolve (get-scape h :stream) "erics-streamscape")))
        (is (= (contents r :password) "pass") )
        (is (= (contents r :data) {}) )
        (is (= (s-> key->all (get-scape r :matrice)) [1]) )))
    
    (testing "host-user"
      (let [addr (s-> self->host-user h "zippy")
            u (get-receptor h addr)]
        (is (= ["zippy"] (s-> key->all (get-scape h :user))))
        (is (= addr (s-> self->host-user h "zippy")))
        (is (= (contents u :name) "zippy"))))

    (testing "interface->send-signal"
      (let [addr (s-> interface->send-signal h {:signal "self->host-user" :from "zippy" :to 0 :params "fish"})
            u (get-receptor h addr)
            ]
        (is (= ["fish" "zippy"] (s-> key->all (get-scape h :user))))
        (is (= addr (s-> self->host-user h "fish")))
        (is (= (contents u :name) "fish")))
      )
    (testing "interface->authenticate"
      (let [i (receptor :some-interface h)
            s (--> interface->authenticate i h {:user "zippy"})
            sessions (get-scape h :session)
            {user-addr :user time :time interface :interface} (s-> key->resolve sessions s)
            ]
        (is (= user-addr (resolve-name h "zippy")))
        (is (= interface :some-interface))
        (is (thrown-with-msg? RuntimeException #"authentication failed for user: squid"
              (--> interface->authenticate i h {:user "squid"})))
        ))
    (testing "interface->new-user"
      (let [i (receptor :some-interface h)]
        (is (thrown-with-msg? RuntimeException #"username 'zippy' in use"
              (--> interface->new-user i h {:user "zippy"})))
        (is (= (--> interface->new-user i h {:user "eric"}) (resolve-name h "eric")))
        ))
    (testing "restore"
      (is (=  (state h true) (state (restore (state h true) nil) true))))
    ))
