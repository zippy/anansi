(ns anansi.test.receptor.host
  (:use [anansi.receptor.host] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape])
  (:use [clojure.test]))

(deftest host
  (let [h (receptor host nil)]
    (testing "host-room"
      (let [addr (s-> self->host-room h {:name "the room" :password "pass" :matrice-address 1 :data {:background-url "http://someure.com/pic.jpg"}})
            r (get-receptor h addr)]
        (is (= addr (s-> key->resolve (contents h :room-scape) "the room")))
        (is (= (contents r :password) "pass") )
        (is (= (contents r :data) {:background-url "http://someure.com/pic.jpg"}) )
        (is (= (s-> key->all (get-scape r :matrice-scape)) [1]))))

    (testing "host-streamscape"
      (let [addr (s-> self->host-streamscape h {:name "erics-streamscape" :password "pass" :matrice-address 1 :data {}})
            r (get-receptor h addr)]
        (is (= addr (s-> key->resolve (contents h :stream-scape) "erics-streamscape")))
        (is (= (contents r :password) "pass") )
        (is (= (contents r :data) {}) )
        (is (= (s-> key->all (get-scape r :matrice-scape)) [1]) )))
    
    (testing "host-user"
      (let [addr (s-> self->host-user h "zippy")
            u (get-receptor h addr)]
        (is (= ["zippy"] (s-> key->all (contents h :user-scape))))
        (is (= addr (s-> self->host-user h "zippy")))
        (is (= (contents u :name) "zippy"))))
    (testing "restore"
      (is (=  (state h true) (state (restore (state h true) nil) true))))
    ))
