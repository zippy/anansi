(ns anansi.test.server
  (:use [anansi.ceptr]
        [anansi.test.helpers]
        [anansi.receptor.host]
        [anansi.server-constants]
        )
  (:use [anansi.server] :reload)

  (:use [anansi.user]
        )
  
  (:use [clojure.test]
        [clojure.contrib.io :only [writer]]
        [clojure.contrib.json :only [json-str]]))

(deftest server-test
  (let [[client-stream server-stream] (make-client-server)]
    (testing "welcome"
      (Thread/sleep 100)
      (is  (.endsWith (.toString server-stream) "\nWelcome to the Anansi sever.\n\nEnter your user name: ")))
    (testing "login"
      (.write client-stream "bob\n")
      (Thread/sleep 2000)
      (is (re-find #"\{\"status\":\"ok\", \"result\":\{\"user-address\":([0-9]+), \"host-address\":0\}\}\n> " (.toString server-stream) )))
    (testing "creating a room"
      (.write client-stream (str "ss "(json-str {:to 0 :signal "self->host-room" :params {:name "the-room" :password "pass" :matrice-address 5}}) "\n"))
      (Thread/sleep 1000)
      (is (re-find #"\{\"status\":\"ok\", \"result\":[0-9]+\}\n> $" (.toString server-stream))))
    (let [[m room-addr] (re-find #"\{\"status\":\"ok\", \"result\":([0-9]+)\}\n> $" (.toString server-stream))] 
      (testing "entering a room"
        (let [c (str "ss " (json-str {:to (Integer. room-addr) :signal "door->enter" :params {:password "pass" :name "bob", :data {:image-url "http://images.com/img.jpg"}}}) "\n")]
          (.write client-stream c)
          (Thread/sleep 1000)
          (is (re-find #"\{\"status\":\"ok\", \"result\":([0-9]+)\}\n> $" (.toString server-stream)))))
      (testing "sleeping an occupant"
        (let [[m o-addr] (re-find #"\{\"status\":\"ok\", \"result\":([0-9]+)\}\n> $" (.toString server-stream))]
          (.write client-stream (str "ss " (json-str {:to (Integer. room-addr) :signal "matrice->update-status" :params {:addr (Integer. o-addr) :status "sleepy"}}) "\n"))
          (Thread/sleep 1000)
          (is (re-find #"\{\"status\":\"ok\", \"result\":null\}\n> $" (.toString server-stream))))))
    )
  
)
