(ns anansi.test.receptor.facilitator
  (:use [anansi.receptor.facilitator] :reload)
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [clojure.test]))

(deftest facilitator
  (let [f (receptor facilitator nil "some-url")
        stick (contents f :stick-scape)]
    (testing "contents"
      (is (= (contents f :image-url) "some-url")))
    (testing "you get the stick if nobody has it when you say you want it"
      (is (= [] (address->resolve stick :want-it)))
      (is (= [] (address->resolve stick :have-it)))
      (participant->request-stick f 1)
      (is (= [] (address->resolve stick :want-it)))
      (is (= [1] (address->resolve stick :have-it))))
    (testing "you get the stick if someone releases it to you when you say you want it"
      (participant->request-stick f 2)
      (is (= [2] (address->resolve stick :want-it)))
      (is (= [1] (address->resolve stick :have-it)))
      (participant->release-stick f 1)
      (is (= [] (address->resolve stick :want-it)))
      (is (= [2] (address->resolve stick :have-it))))
    (testing "no-one has the stick if someone releases it when no-one has said they want it"
      (participant->release-stick f 2)
      (is (= [] (address->resolve stick :want-it)))
      (is (= [] (address->resolve stick :have-it))))
    (testing "you get the stick if the matrice gives it to you"
      (matrice->give-stick f 1)
      (is (= [1] (address->resolve stick :have-it)))
      (matrice->give-stick f 2)
      (is (= [2] (address->resolve stick :have-it)))
      )
    (testing "you say you want the stick if the matrice says you want the stick")

    ))
