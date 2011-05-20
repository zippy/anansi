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
      (is (= [] (s-> address->resolve stick :want-it)))
      (is (= [] (s-> address->resolve stick :have-it)))
      (s-> participant->request-stick f 1)
      (is (= [] (s-> address->resolve stick :want-it)))
      (is (= [1] (s-> address->resolve stick :have-it))))
    (testing "you get the stick if someone releases it to you when you say you want it"
      (s-> participant->request-stick f 2)
      (is (= [2] (s-> address->resolve stick :want-it)))
      (is (= [1] (s-> address->resolve stick :have-it)))
      (s-> participant->release-stick f 1)
      (is (= [] (s-> address->resolve stick :want-it)))
      (is (= [2] (s-> address->resolve stick :have-it))))
    (testing "no-one has the stick if someone releases it when no-one has said they want it"
      (s-> participant->release-stick f 2)
      (is (= [] (s-> address->resolve stick :want-it)))
      (is (= [] (s-> address->resolve stick :have-it))))
    (testing "if you said you want the stick, and then you release the stick, you don't want it any more"
      (s-> participant->request-stick f 1)
      (s-> participant->request-stick f 2)
      (is (= [2] (s-> address->resolve stick :want-it)))
      (is (= [1] (s-> address->resolve stick :have-it)))
      (s-> participant->release-stick f 2)
      (is (= [] (s-> address->resolve stick :want-it)))
      (is (= [1] (s-> address->resolve stick :have-it)))
      )
    (testing "you get the stick if the matrice gives it to you"
      (s-> matrice->give-stick f 1)
      (is (= [1] (s-> address->resolve stick :have-it)))
      (s-> matrice->give-stick f 2)
      (is (= [2] (s-> address->resolve stick :have-it)))
      )
    (testing "state"
      (is (= (:image-url (state f true))
             "some-url")))
    (testing "restore"
      (is (=  (state f true) (state (restore (state f true) nil) true))))
    (testing "you say you want the stick if the matrice says you want the stick")

    ))
