(ns anansi.test.receptor.object
  (:use [anansi.receptor.object] :reload)
  (:use [anansi.ceptr])
  (:use [clojure.test]))

(deftest object
  (let [o (receptor :object nil "some-url")]
    (testing "contents"
      (is (= (contents o :image-url) "some-url")))
    ))
