(ns anansi.test.receptor.host-interface.commands

  (:use [anansi.receptor.host-interface.commands] :reload)
  (:use [anansi.ceptr])
  (:use [anansi.receptor.host]
        [anansi.receptor.scape])
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]))

(def some-interface-def (receptor-def "some-interface"))

(deftest commands
  (let [h (make-receptor host-def nil {})
        i (make-receptor some-interface-def h {})]
    (binding [*err* (java.io.PrintWriter. (writer "/dev/null"))]
      (testing "execute"  
        (is (= {:status :error :result "authentication failed for user: eric"}
               (execute h i "authenticate" {:user "eric"})) )))
    (testing "authenticate"
      (is (thrown-with-msg? RuntimeException #"authentication failed for user: eric"
            (authenticate h i {:user "eric"}))))
    (let [n-addr (new-user h i {:user "eric"})]
      (testing "new-user"
        (is (= n-addr (resolve-name h "eric"))))
      (testing "send"
        (let [session (authenticate h i {:user "eric"})]
          (is (re-find #"^[0-9a-f]+$" session))
          (is (= (send-signal h i {:signal "host-user" :aspect "self" :prefix "receptor.host" :session session :to 0 :params "boink"})
                 (resolve-name h "boink")))
          (testing "get-state"
            (is (= {:name "eric", :fingerprint :anansi.receptor.user.user, :address n-addr, :changes 0} (get-state h i {:receptor n-addr})))
            )
        ))
    ))
)
