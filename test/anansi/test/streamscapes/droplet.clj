(ns anansi.test.streamscapes.droplet
  (:use [anansi.streamscapes.droplet] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest droplet
  (let [r (receptor :some-parent nil)
        o (make-receptor droplet-def r "some-id" "from-addr" "to-addr" :some-channel {:part1 "address of part1 grammar"} {:part1 "part1 content"})]
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
