(ns anansi.test.receptor.host-interface.http
  (:use [anansi.receptor.host-interface.http] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.host])
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]
        [aleph.http]
        [aleph.formats]
        [lamina.core]))

(deftest http-interface
  
  (let [h (receptor :host nil)
        r (receptor :http-host-interface h {})]
    (testing "server"
      (is (thrown-with-msg? RuntimeException #"Server not started."
            (--> interface->stop h r)))
      (--> interface->start h r {:port 12345})
      (is (re-find #"^class aleph.http.server" (str (class (contents r :server)))))
      (let [resp (sync-http-request {:method :get, :url "http://localhost:12345"})]
        (is (= (bytes->string (:body resp)) "Welcome to the Anansi sever."))
        )
      (is (thrown-with-msg? RuntimeException #"Server already started."
            (--> interface->start h r {:port 12345})))
      (--> interface->stop h r)
      )
    ))
 
