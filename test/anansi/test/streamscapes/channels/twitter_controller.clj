(ns anansi.test.streamscapes.channels.twitter-controller
  (:use [anansi.streamscapes.channels.twitter-controller] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.ident :only [ident-def]]
        [anansi.streamscapes.channel]

        [anansi.streamscapes.channels.twitter-bridge-in :only [twitter-bridge-in-def]]
        )
  (:use [midje.sweet]))

(let [m (make-receptor user-def nil "eric")
      r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
      eric (make-receptor ident-def r {:attributes {:name "Eric"}})
      channel-address (s-> setup->new-channel r {:type :twitter, :name :twitter, :screen-name "zippy314"})
      cc (get-receptor r channel-address)
      [controller-address control-signal] (get-controller cc)
      b (get-receptor cc controller-address)]
  (facts "about twitter controller"
    (receptor-state b false) => (contains {:screen-name "zippy314" :fingerprint :anansi.streamscapes.channels.twitter-controller.twitter-controller})
    (scape-size (get-scape r :id)) => 0
    (s-> channel->control b {:command :check})
    (scape-size (get-scape r :id)) => 1
    ))

