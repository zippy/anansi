(ns anansi.test.streamscapes.droplet

  (:use [anansi.streamscapes.droplet] :reload
        [anansi.streamscapes.streamscapes :only [streamscapes-def]]
        [anansi.receptor.user :only [user-def]]
        [anansi.receptor.host :only [host-def]])
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest droplet
  (let [
        m (make-receptor user-def nil "eric")
        h (make-receptor host-def nil {})
        r (make-receptor streamscapes-def h {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
        o (make-receptor droplet-def r "some-id" "from-addr" "to-addr" :some-channel {:part1 "address of part1 grammar"} {:part1 "part1 content"})
        ]
    (testing "contents"
      (is (= "some-id" (contents o :id)))
      (is (= "from-addr" (contents o :from)))
      (is (= "to-addr" (contents o :to)))
      (is (= :some-channel (contents o :channel)))
      (is (= {:part1 "address of part1 grammar"} (contents o :envelope)))
      (is (= {:part1 "part1 content"} (contents o :content)))
      )
    (testing "droplets with auto-id creation"
      (let [d (make-receptor droplet-def r nil "from-addr" "to-addr" :some-channel {:part1 "address of part1 grammar"} {:part1 "part1 content"})]
        (is (=  (str (address-of r) "." (address-of d)) (contents d :id)))))
    (testing "restore"
      (is (=  (receptor-state o true) (receptor-state (receptor-restore (receptor-state o true) nil) true))))
    )
  )
