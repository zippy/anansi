(ns anansi.test.receptor.host-interface.commands

  (:use [anansi.receptor.host-interface.commands] :reload)
  (:use [anansi.ceptr])
  (:use [anansi.receptor.host]
        [anansi.receptor.scape])
  (:use [clojure.test]))

(deftest commands
  (let [h (receptor :host nil)
        i (receptor :some-interface h {})]
    (testing "authenticate"
      (is (thrown-with-msg? RuntimeException #"authentication failed for user: eric"
            (authenticate h i "eric"))))
    (testing "new-user"
      (let [n-addr (new-user h i "eric")]
        (is (= n-addr (resolve-name h "eric"))))
      )
    ))
