(ns anansi.test.receptor.host-interface.commands

  (:use [anansi.receptor.host-interface.commands] :reload)
  (:use [anansi.ceptr])
  (:use [anansi.receptor.host]
        [anansi.receptor.scape])
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]))
(comment = {:status :error :result "authentication failed for user: eric"}
            (execute h i "authenticate" {:user "eric"}))
(deftest commands
  (let [h (receptor :host nil)
        i (receptor :some-interface h {})]
    (binding [*err* (java.io.PrintWriter. (writer "/dev/null"))]
      (testing "execute"  
        (is (= {:status :error :result "authentication failed for user: eric"}
               (execute h i "authenticate" {:user "eric"})) )))
    (testing "authenticate"
      (is (thrown-with-msg? RuntimeException #"authentication failed for user: eric"
            (authenticate h i {:user "eric"}))))
    (testing "new-user"
      (let [n-addr (new-user h i {:user "eric"})]
        (is (= n-addr (resolve-name h "eric"))))
      )
    (comment testing "send"
      (id (= (resolve-name h "boink") (send-signal h i {:signal "self->host-user" :session xxx :to (address-of h) :params {"boink"}}))))
    ))
