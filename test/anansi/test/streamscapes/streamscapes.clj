(ns anansi.test.streamscapes.streamscapes
  (:use [anansi.streamscapes.streamscapes] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]   
        [anansi.receptor.user])
  (:use [clojure.test]))

(deftest streamscapes
  (let [m (receptor user nil "eric" nil)
        u (receptor user nil "zippy" nil)
        r (receptor streamscapes nil (address-of m) "password" {:datax "x"})
        aspects (get-scape r :aspect)
        ids (get-scape r :id)
        ]
    (testing "initialization"
      (is (= [(address-of m)] (s-> address->resolve (get-scape r :matrice) :matrice)))
      (is (= {:datax "x"} (contents r :data)))
      )
    (testing "droplets"
      (let [droplet-address (s-> matrice->incorporate r {:id "some-unique-id" :from "from-addr" :to "to-addr" :aspect :some-aspect :envelope {:part1 "address of part1 grammar"} :content {:part1 "part1 content"}})
            d (get-receptor r droplet-address)]
        (are [x y] (= x y)
             (contents d :id) "some-unique-id"
             (contents d :from) "from-addr"
             (contents d :to) "to-addr"
             (contents d :aspect) :some-aspect
             (contents d :envelope) {:part1 "address of part1 grammar"}
             (contents d :content) {:part1 "part1 content"}
             (address-of d) droplet-address
             :some-aspect (s-> key->resolve aspects droplet-address)
             "some-unique-id" (s-> key->resolve ids droplet-address)
             [droplet-address] (s-> address->resolve aspects :some-aspect)
             [droplet-address] (s-> address->resolve ids "some-unique-id")
             )))))
