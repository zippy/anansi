(ns anansi.test.receptor.host-interface.http
  (:use [anansi.receptor.host-interface.http] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.host])
  (:use [midje.sweet])
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]
        [aleph.http]
        [aleph.formats]
        [lamina.core]))

(defn api-req [command params]
  (sync-http-request {:method :post,
                      :url "http://localhost:12345/api"
                      :auto-transform :true
                      :body (encode-json->bytes {:cmd command :params params})
                      :probes {:errors nil-channel}}
                     1000)
  )
(deftest http-interface
  
  (let [h (make-receptor host-def nil {})
        r (make-receptor http-def h {})
        z-addr (s-> self->host-user h "zippy")]
    (testing "starting interface"
      (is (thrown-with-msg? RuntimeException #"Server not started."
            (--> interface->stop h r)))
      (--> interface->start h r {:port 12345})
      (is (re-find #"^class aleph.http.server" (str (class (contents r :server)))))
      (is (thrown-with-msg? RuntimeException #"Server already started."
            (--> interface->start h r {:port 12345})))
      )
    (testing "static files"
      (let [resp (sync-http-request {:method :get, :url "http://localhost:12345/nonexistent"})]
        (is (= 404 (:status resp)))
        (is (= "Not Found" (bytes->string (:body resp))))
        )
      (let [resp (sync-http-request {:method :get, :url "http://localhost:12345/"})]
        (is (= "<html><title>Anansi</title><body><h2>Welcome to the Anansi sever!</h2><img src=\"web.jpg\" /></body></html>" (bytes->string (:body resp))))
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
      )
    (testing "autostart"
      (let [asr (make-receptor http-def h {:auto-start {:port 12345}})]
        (fact (receptor-state asr false) => (contains {:auto-start {:port 12345}, :fingerprint :anansi.receptor.host-interface.http.http}))
        (is (thrown-with-msg? RuntimeException #"Server already started."
              (--> interface->start h asr {:port 12345})))
        (--> interface->stop h asr))
      )))
 
