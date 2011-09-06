(ns anansi.test.streamscapes.channels.irc-bridge-out
  (:use [anansi.streamscapes.channels.irc-bridge-out] :reload)
  (:use [anansi.streamscapes.channel])
  (:use [anansi.ceptr])
  (:use [anansi.receptor.scape])
  (:use [anansi.streamscapes.streamscapes])
  (:use [clojure.test]))

(deftest irc-bridge-out
  (let [m (receptor :user nil "eric" nil)
        r (receptor :streamscapes nil (address-of m) "password" {:datax "x"})
        eric (receptor :ident r {:name "Eric"})
        cc (receptor :channel r :irc-stream)
        b (receptor :irc-bridge-out cc {})
        irc-idents (get-scape r :irc-ident true)]
    (--> key->set b irc-idents "zippy" (address-of eric))
    ;    (testing "contents" )
    (testing "restore"
      (is (=  (state cc true) (state (restore (state cc true) nil) true))))
    
    ))
