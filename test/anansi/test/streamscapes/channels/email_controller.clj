(ns anansi.test.streamscapes.channels.email-controller
  (:use [anansi.streamscapes.channels.email-controller] :reload)
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.receptor.user :only [user-def]]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.contact :only [contact-def]]
        [anansi.streamscapes.channel]

        [anansi.streamscapes.channels.email-bridge-in :only [email-bridge-in-def]]
        )
  (:use [midje.sweet]))

(let [m (make-receptor user-def nil "eric")
      r (make-receptor streamscapes-def nil {:matrice-addr (address-of m) :attributes {:_password "password" :data {:datax "x"}}})
      eric (make-receptor contact-def r {:attributes {:name "Eric"}})
      channel-address (s-> setup->new-channel r {:type :email, :name :email,
                                                 :in {:host "mail.harris-braun.com" :account "eric@harris-braun.com" :password "pass" :protocol "pop3" :port "110"}
                                                 })
      cc (get-receptor r channel-address)
      [controller-address control-signal] (get-controller cc)
      b (get-receptor cc controller-address)]
  (facts "about email controller"
    (receptor-state b false) => (contains {:fingerprint :anansi.streamscapes.channels.email-controller.email-controller})
    (s-> channel->control b {:command :check}) => (throws javax.mail.AuthenticationFailedException "Authentication failed.")
    ))

