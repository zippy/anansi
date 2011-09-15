(ns anansi.test.receptor.host-interface.http
  (:use [anansi.receptor.host-interface.http] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.host])
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]
        [aleph.http]
        [aleph.formats]
        [lamina.core]))

(defn api-req [command params]
  (sync-http-request {:method :post, :url "http://localhost:12345/api", :auto-transform :true, :body (encode-json->bytes {:cmd command :params params}) } 1000)
  )
(deftest http-interface
  
  (let [h (receptor :host nil)
        r (receptor :http-host-interface h {})
        z-addr (s-> self->host-user h "zippy")]
    (testing "starting interface"
      (is (thrown-with-msg? RuntimeException #"Server not started."
            (--> interface->stop h r)))
      (--> interface->start h r {:port 12345})
      (is (re-find #"^class aleph.http.server" (str (class (contents r :server)))))
      (is (thrown-with-msg? RuntimeException #"Server already started."
            (--> interface->start h r {:port 12345})))
      (let [resp (sync-http-request {:method :get, :url "http://localhost:12345"})]
        (is (= (bytes->string (:body resp)) "Welcome to the Anansi sever."))
        )
      )
    (testing "authenticate"
      (is (= (:body (api-req "authenticate" {:user "eric"})) {:status "error", :result "authentication failed for user: eric"}))
      (let [b (:body (api-req "authenticate" {:user "zippy"}))
             session (:result b)]
        (is (= "ok" (:status b)))
        (is (re-find #"^[0-9a-f]+$" session))
        (testing "send signal"
          (is (= {:status "ok" :result "Hi 7! This is the host."} (:body (api-req "send-signal" {:to 0 :prefix "receptor.host" :aspect "ceptr" :signal "ping" :session session}))))))
      )
    (testing "stopping the interface"
      (--> interface->stop h r)
      (is (= nil (contents r :server)))
      (is (thrown-with-msg? java.net.ConnectException #"Connection refused" (api-req "some-command" {:x 1})))
      )))
 
