(ns anansi.test.streamscapes.droplet
  (:use [anansi.streamscapes.droplet] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest droplet
  (let [o (receptor droplet nil "from-addr" "to-addr" :some-aspect {:part1 "address of part1 grammar"} {:part1 "part1 content"})]
    (testing "contents"
      (is (= "from-addr" (contents o :from)))
      (is (= "to-addr" (contents o :to)))
      (is (= :some-aspect (contents o :aspect)))
      (is (= {:part1 "address of part1 grammar"} (contents o :envelope)))
      (is (= {:part1 "part1 content"} (contents o :content)))
      )
    (testing "restore"
      (is (=  (state o true) (state (restore (state o true) nil) true))))
    ))
