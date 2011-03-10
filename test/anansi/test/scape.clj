(ns anansi.test.scape
  (:use [anansi.scape] :reload)
  (:use [clojure.test]))
  
(deftest hash-scape
  (let [scape (scape-set (make-hash-scape) :a 1)]
    (testing "ScapeStore protocol"
      (is (= 1 (scape-get scape {:key :a})))
      (is (= [:a] (scape-get scape {:address 1})))
      (is (= (make-hash-scape) (scape-delete scape {:key :a})))
      (is (= (make-hash-scape) (scape-delete scape {:address 1})))
      (is (= [:a] (into [] (scape-keys scape))))
      (is (= [1] (into [] (scape-addresses scape))))
      (is (= {:type "HashScape", :contents {:a 1}} (scape-dump scape))))
    ))

(deftest scapes
  (testing "make-scapes-ref"
    (is (= {:scape-1 (make-hash-scape) :scape-2 (make-hash-scape)}
           @(make-scapes-ref :scape-1 :scape-2))))
  (testing "making scapes"
    (is (= {} (make-hash-scape)))
    (is (= (sorted-map) (make-hash-scape (sorted-map)))))
  )

(deftest serialization
  (let [scapes {:scape-1 (scape-set (make-hash-scape) :a 1), :scape-2 (make-hash-scape)}]
    (testing "dump-scapes"
      (is (= {:scape-1 {:type "HashScape" :contents {:a 1}}, :scape-2 {:type "HashScape" :contents {}} } (dump-scapes scapes))))
    (testing "unserialize-scape"
      (is (= scapes (unserialize-scapes (dump-scapes scapes))
           )))))

(deftest angle-scape
  (let [seat-scape (-> (make-hash-scape) (scape-set 0 "eric") (scape-set 1 "art"))]
    (testing "calculate-angles"
      (is (= {0 "art", 180 "eric"} (calculate-angles seat-scape))))
    (testing "angle-scape-from-seat-scape"
      (let [angle-scape (angle-scape-from-seat-scape seat-scape)]
        (is (= [180] (scape-get angle-scape {:address "eric"}))))
      )))

