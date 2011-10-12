(ns anansi.test.receptor.host
  (:use [anansi.receptor.host] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user])
  (:use [midje.sweet])
  (:use [clojure.test]))

(def some-interface-def (receptor-def "some-interface"))

(let [h (make-receptor host-def nil {})
      ha (address-of h)]
  (facts "scaping relationships"
    (scape-relationship (get-scape h :room) :key) => :name
    (scape-relationship (get-scape h :room) :address) => :address
    (scape-relationship (get-scape h :user) :key) => :name
    (scape-relationship (get-scape h :user) :address) => :address
    (scape-relationship (get-scape h :stream) :key) => :name
    (scape-relationship (get-scape h :stream) :address) => :address
    (scape-relationship (get-scape h :session) :key) => :sha
    (scape-relationship (get-scape h :session) :address) => :user-addr-time-interface-map
    (scape-relationship (get-scape h :groove) :key) => :name
    (scape-relationship (get-scape h :groove) :address) => :address
    )
  (facts "about default grooves added into the host at startup"
    (let [grooves (get-scape h :groove)
          g (get-receptor h (s-> key->resolve grooves :subject-body-message))]
      (-> (receptor-state h false) :scapes :groove-scape :values :subject-body-message ) => #(= java.lang.Integer (class %))
      (rdef g :fingerprint) => :anansi.streamscapes.groove.groove
      (contents g :grammars) => {:streamscapes {:subject "text/plain", :body "text/html"} :email {:subject "text/plain", :body "text/html"}}
      ))
  (deftest host
    (testing "ping"
      (is (re-find #"Hi [0-9]+! This is the host." (s-> ceptr->ping h nil)))
      )
    (comment testing "ceptr->host-room"
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
        (fact (s-> address->resolve (get-scape h :creator) (address-of h)) => [addr] )
        (is (= addr ))
        (is (= (contents r :_password) "pass") )
        (is (= (contents r :data) {}) )
        (is (= (s-> key->all (get-scape r :matrice)) [1]))))
    
    (facts "about hosting grooves"
      (let [addr (s-> self->host-groove h {:name "a-groove" :grammars {:streamscapes "some-grammar-spec" }})
            r (get-receptor h addr)
            qn (keyword (str (address-of h) ".a-groove"))]
        (s-> key->resolve (get-scape h :groove) qn) => addr
        (contents r :grammars) => {:streamscapes "some-grammar-spec"}
        (s-> self->host-groove h {:name "a-groove"}) => (throws RuntimeException (str "A groove already exists with the name: " qn))
        ))
    
    (testing "self->host-user"
      (let [addr (s-> self->host-user h "zippy")
            u (get-receptor h addr)]
        (is (= ["zippy"] (s-> key->all (get-scape h :user))))
        (is (= addr (s-> self->host-user h "zippy")))
        (is (= (contents u :name) "zippy"))))

    (testing "command->send-signal"
      (let [user-addr (resolve-name h "zippy")
            _ (s-> key->set (get-scape h :session) "1234" {:user user-addr})
            result (s-> command->send-signal h {:prefix "receptor.host" :aspect "ceptr" :signal "ping" :session "1234" :to 0 :params nil})
            ]
        (is (= result (str "Hi " user-addr "! This is the host.")))
        (is (thrown-with-msg? RuntimeException #"Unknown signal: receptor.host.ceptr->pong"
              (s-> command->send-signal h {:signal "pong" :aspect "ceptr" :prefix "receptor.host" :session "1234" :to 0 :params nil}))
            )
        (is (thrown-with-msg? RuntimeException #"Unknown session: 12345"
              (s-> command->send-signal h {:signal "ping" :aspect "ceptr" :prefix "receptor.host" :session "12345" :to 0 :params nil}))
            )))
 
    (testing "command->authenticate"
      (let [i (make-receptor some-interface-def h {})
            {s :session} (--> command->authenticate i h {:user "zippy"})
            sessions (get-scape h :session)
            {user-addr :user time :time interface :interface} (s-> key->resolve sessions s)
            ]
        (is (= user-addr (resolve-name h "zippy")))
        (is (= interface :anansi.test.receptor.host.some-interface))
        (is (thrown-with-msg? RuntimeException #"authentication failed for user: squid"
              (--> command->authenticate i h {:user "squid"})))
        ))
    (testing "command->new-user"
      (let [i (make-receptor some-interface-def h {})]
        (is (thrown-with-msg? RuntimeException #"username 'zippy' in use"
              (--> command->new-user i h {:user "zippy"})))
        (is (= (--> command->new-user i h {:user "eric"}) (resolve-name h "eric")))
        ))
    (testing "command->get-state"
      (let [i (make-receptor some-interface-def h {})
            addr (resolve-name h "eric")]
        (is (thrown-with-msg? RuntimeException #"unknown receptor: 0"
              (--> command->get-state i h {:receptor "0"})))
        (is (= {:name "eric", :fingerprint :anansi.receptor.user.user, :address addr, :changes 0} (--> command->get-state i h {:receptor addr}) ))
        ))
    (comment facts "about restoring serialized receptor"
      (let [state (receptor-state h true)]
        state => (receptor-state (receptor-restore state nil) true)
        ))))
(facts "about querys"
  (let [h (make-receptor host-def nil {})
        i (make-receptor some-interface-def h {})
        z (--> command->new-user i h {:user "zippy"})
        s (--> command->new-user i h {:user "sam"})
        j (--> command->new-user i h {:user "jane"})
        ]
    (set (keys (:receptors (--> command->get-state i h {:receptor 0 :query {:scape-query {:scape :user :query [">" "s"]}}})))) => #{s z}
    (let [state (--> command->get-state i h {:receptor 0 :query {:scape-order {:scape :user :limit 1 :offset 1}}})]
      (:receptor-order state) => [s]
      (set (keys (:receptors state))) => (contains #{s})
      (set (keys (:receptors state))) =not=> (contains #{j z})
      )
    (keys (--> command->get-state i h {:receptor 0 :query {:partial {:scapes true}}})) => (just :scapes)
    (let [q (--> command->get-state i h {:receptor 0 :query {:partial {:scapes {:groove-scape true} :address true}}})]
      (keys q) => (just :scapes :address)
      (keys (:scapes q)) => (just :groove-scape)
      )
    )
  )
