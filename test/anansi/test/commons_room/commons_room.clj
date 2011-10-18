;(ns anansi.test.commons-room.commons-room
;  (:use [anansi.commons-room.commons-room] :reload)
;  (:use [anansi.ceptr]
;        [anansi.receptor.scape]
;        [anansi.receptor.user])
;  (:use [clojure.test]))
;(comment
;  (deftest commons-room
;    (let [m (make-receptor user-def nil "eric")
;          u (make-receptor user-def nil "zippy")
;          u-art (make-receptor user-def nil "art")
;          r (receptor :commons-room nil (address-of m) "password" {:room-name "fun house"})
;          occupants (get-scape r :occupant)
;          coords (get-scape r :coords)
;          status (get-scape r :status)
;          chairs (get-scape r :chair)]
;      (set! *print-level* 10)
;      (testing "initialization"
;        (is (= [(address-of m)] (s-> address->resolve (get-scape r :matrice) :matrice)))
;        (is (= {:room-name "fun house"} (contents r :data)))
;        )
;      (testing "incorporate"
;        (let [flower-address (s-> matrice->incorporate r :flower "http://images.com/flower.jpg" 0 0)
;              chicken-address (s-> matrice->incorporate r :chicken "http://images.com/chicken.jpg" 0 1)]
;          (are [x y] (= x y)
;               (address-of (get-receptor r flower-address)) flower-address
;               [[0 0]] (s-> address->resolve coords flower-address)
;               [[0 1]] (s-> address->resolve coords chicken-address)
;               flower-address (s-> key->resolve coords [0 0])
;               )
;          (is (= flower-address (s-> key->resolve coords [0 0])))
;          (is (not= flower-address chicken-address))))
;      (testing "failed door entrance"
;        (is (thrown-with-msg? RuntimeException #"incorrect room password" (s-> door->enter r {:password "wrong" :name  "x" :data {:name "e"}})))
;        )
;      (let [address-of-o (--> door->enter u r {:password "password" :name "zippy" :data {:name "Eric H-B", :image "http://gravatar.com/userimage/x.jpg" :phone "123/456-7890"}})]
;        (testing "door->enter"
;          (is (agent-or-matrice? r (address-of u) address-of-o))
;          (is (agent-or-matrice? r (address-of m) address-of-o))
;          (is (not (agent-or-matrice? r (address-of u-art) address-of-o)))
;          ;; enter event is posted to the door log
;          (let [le (last @(contents r :door-log))]
;            (is (= "zippy" (:who le)))
;            (is (= "entered" (:what le)))
;            (is (re-find #"^... ... [0-9][0-9] [0-9][0-9]:[0-9][0-9]:[0-9][0-9] ... 20[0-9][0-9]" (:when le))))
;          ;; sender of enter is in the agent scape for the occupant
;          (is (= (address-of u) (s-> key->resolve (get-scape r :agent) address-of-o)))
;          (comment is (= (s-> key->resolve (get-scape r :seat) 0) address-of-o))
;          (is (= (s-> key->all occupants) ["zippy"] ))
;          ;; entering again fails
;          (is (thrown-with-msg? RuntimeException #"'zippy' is already in the room" (s-> door->enter r {:password "password" :name  "zippy" :data {:name "e"}})))
;          ;; on entering occupant is present
;          (is (= (s-> key->resolve status address-of-o) :present))
;          )
;        (testing "make make agent"
;          ;; refuse if not from matrice
;          (is (thrown-with-msg? RuntimeException #"not matrice" (-->  matrice->make-agent u r {:occupant 1 :addr 1})))
;          (--> matrice->make-agent m r {:occupant address-of-o :addr (address-of u-art)} )
;          (is (= (address-of u-art) (s-> key->resolve (get-scape r :agent) address-of-o)))
;          (--> matrice->make-agent m r {:occupant address-of-o :addr (address-of u)} )
;          )
;
;        (testing "status"
;          (let [addr (s-> key->resolve occupants "zippy")]
;            (is (= (s-> key->resolve status addr) :present))
;            (is (nil? (--> matrice->update-status m r {:addr addr :status "away"} )))
;            (is (= (s-> key->resolve status addr) :away))
;            (--> matrice->update-status u r {:addr addr :status "tired"} )
;            (is (= (s-> key->resolve status addr) :tired))
;            (is (= [:tired] (s-> address->all status)))
;            (is (thrown-with-msg? RuntimeException #"no agency" (--> matrice->update-status u-art r {:addr addr :status "asleep"})))
;            )
;          )
;        (testing "update-data"
;          (is (thrown-with-msg? RuntimeException #"no agency" (--> occupant->update-data u-art r {:name "zippy"})))
;          (--> occupant->update-data u r {:name "zippy" :data {:name "E E B"}})
;          (is (= {:name "E E B"} (:data (state (get-receptor r address-of-o) true))))
;          (--> occupant->update-data u r {:name "zippy" :data "Herbert" :key :name})
;          )
;        (testing "door-leave"
;          ;; refuse leave if not from agent
;          (is (thrown-with-msg? RuntimeException #"no agency" (--> door->leave u-art r "zippy")))
;          (is (= nil (--> door->leave u r "zippy")))
;          ;; leave event is posed to the door log
;          (let [le (last @(contents r :door-log))]
;            (is (= "zippy" (:who le)))
;            (is (= "left" (:what le)))
;            (is (re-find #"^... ... [0-9][0-9] [0-9][0-9]:[0-9][0-9]:[0-9][0-9] ... 20[0-9][0-9]" (:when le))))
;          (comment is (= nil (get-scape (contents r :seat) :map)))
;          (is (= [] (s-> address->resolve occupants address-of-o)))
;          (is (= (s-> key->all (get-scape r :occupant)) [] ))
;          (is (= (s-> key->all (get-scape r :agent)) [] ))
;          (is (= (s-> key->all (get-scape r :status)) [] ))
;          (is (nil? (get-receptor r address-of-o)))
;          ;; leave works if from matrice
;          (--> door->enter u r {:password "password" :name "zippy" :data {:name "Eric"}})
;          (--> door->leave m r "zippy")))
;      (let [addr (--> door->enter u r {:password "password" :name "zippy" :data {:name "Eric"}})]
;        (testing "move"
;          ;; refuse if not from matrice
;          (is (thrown-with-msg? RuntimeException #"not matrice" (-->  matrice->move u r {:addr addr :x 100 :y 100})))
;          (--> matrice->move m r {:addr addr :x 100 :y 100} )
;          (is (= addr (s-> key->resolve coords [100 100])))
;          (--> matrice->move m r {:addr addr :x 20 :y 20})
;          (is (= [[20 20]] (s-> address->resolve coords addr))))
;        (testing "sit"
;          ;; refuse if not from matrice
;          (is (thrown-with-msg? RuntimeException #"not matrice" (-->  matrice->sit u r {:addr addr :chair 1})))
;          (--> matrice->sit m r {:addr addr :chair 1} )
;          (is (= addr (s-> key->resolve chairs 1)))
;          (--> matrice->sit m r {:addr addr :chair 4})
;          (is (= [4] (s-> address->resolve chairs addr)))))
;      (testing "talking-stick"
;        (--> door->enter u-art r {:password "password" :name "art" :data {:name "Art"}})
;        (let [f (contents r :talking-stick)
;              s (get-scape f :stick)
;              zippy_addr (s-> key->resolve occupants "zippy")
;              art_addr (s-> key->resolve occupants "art") ]
;          ;; refuse if not from agent
;          (is (thrown-with-msg? RuntimeException #"no agency" (--> stick->request u-art r "zippy")))
;          (--> stick->request u r "zippy")
;          (is (= [zippy_addr] (s-> address->resolve s :have-it)))
;          (--> stick->request u-art r "art")
;          (--> stick->release u r "zippy")
;          (is (= [art_addr] (s-> address->resolve s :have-it)))
;          (--> stick->give u r "zippy")
;          (is (= [zippy_addr] (s-> address->resolve s :have-it)))
;          ))
;      (testing "state-pretty"
;        ;; cant actually test the contents because addresses change...
;        (is (= (into #{} (keys  (state r false)))
;               #{:receptors :matrices :scapes :type :talking-stick :address :changes :occupants :data}))
;        (is (=  {"art" {:name "Art"}, "zippy" {:name "Eric"}} (:occupants (state r false)))))
;      (testing "state-full"
;        (is (= (into #{} (keys  (state r true)))
;               #{:data :scapes-scape-addr :receptors :type :address :door-log :door :matrice-scape :talking-stick :password :changes}))
;        (comment is (= nil (restore (state r true) nil))))
;      (testing "restore"
;        (is (=  (state r true) (state (restore (state r true) nil) true))))
;      (testing "make matrice"
;        ;; refuse if not from matrice
;        (is (thrown-with-msg? RuntimeException #"not matrice" (-->  matrice->make-matrice u r {:addr 1})))
;        (--> matrice->make-matrice m r {:addr (address-of u)} )
;        (is (= :matrice (s-> key->resolve (get-scape r :matrice) (address-of u))))
;        (is (= [(address-of m) (address-of u)] (s-> address->resolve (get-scape r :matrice) :matrice)))
;        )
;      ))
;
;  (comment set-person-image )
;  (comment stick creates slots for every occupant in the room.  facilitation receptor)
;)
