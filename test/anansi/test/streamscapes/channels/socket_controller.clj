(ns anansi.test.streamscapes.channels.socket-controller
  (:use [anansi.streamscapes.channels.socket-controller] :reload)
  (:use [anansi.streamscapes.channel])
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.streamscapes.streamscapes])
  (:use [clojure.test])
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader)))


(declare conn-handler)

(def *result* (ref {}))

(defn connect [host port]
  (let [socket (Socket. host port)
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:in in :out out :socket socket})]
    (doto (Thread. #(conn-handler conn)) (.start))
    conn))

(defn write [conn msg]
  (doto (:out @conn)
    (.println (str msg "\r"))
    (.flush)))

(defn conn-handler [conn]
  (while 
      (nil? (:exit @conn))
    (let [msg (.readLine (:in @conn))]
      (if (not (nil? msg)) (dosync (alter *result* assoc :val msg))))))

(deftest socket-controller
  (let [m (receptor :user nil "eric" nil)
        r (receptor :streamscapes nil (address-of m) "password" {:datax "x"})
        eric (receptor :ident r {:name "Eric"})
        house (receptor :ident r {:name "my-house"})
        channel-address (s-> matrice->make-channel r {:name :socket-stream
                                                      :receptors {:socket-controller {:role :controller :signal channel->control :params {:port 3141 :input-function (fn [input] (println (str "processed: " input)))}}}
                                                          })
        cc (get-receptor r channel-address)
        [controller-address control-signal] (get-controller cc)
        b (get-receptor cc controller-address)
        socket-idents (get-scape r :socket-ident true)]
    (--> key->set b socket-idents "127.0.0.1" (address-of house))

    (testing "contents"
      (is (= 3141 (contents b :port))))
    (testing "restore"
      (is (=  (state cc true) (state (restore (state cc true) nil) true))))
    (testing "listening on a socket and receiving a signal on it"
      (is (= (s-> channel->control b {:command :status}) :closed))
      (s-> channel->control b {:command :open})
      (is (= (s-> channel->control b {:command :status}) :open))
      (let [conn (connect "127.0.0.1" 3141)]
        (write conn "test message")
        )
      (while (nil? (:val @*result*)) "idling")
      (is (= (:val @*result*) "processed: test message"))
      (s-> channel->control b {:command :close})
      (is (= (s-> channel->control b {:command :status}) :closed)))
    (testing "unknown control signal"
      (is (thrown-with-msg? RuntimeException #"Unknown control command: :fish" (s-> channel->control b {:command :fish})))
      )))
