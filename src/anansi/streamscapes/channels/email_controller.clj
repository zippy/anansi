(ns
  #^{:author "Eric Harris-Braun"
     :doc "Email Controller receptor"}
  anansi.streamscapes.channels.email-controller
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]
        [anansi.streamscapes.channels.email-bridge-in :only [pull-messages]]
        )
)

(def email-controller-def (receptor-def "email-controller"))

(signal channel control [_r _f control-params]
        (let [{command :command params :params} control-params]
          (condp = command
              :check (let [parent-channel (parent-of _r)
                          [in-bridge-address receive-signal] (get-receiver-bridge parent-channel)
                           ib (get-receptor parent-channel in-bridge-address)]
                       ;THIS IS WAY CHEATING!!  The controller needs
                       ;to send this as a signal, not call it as a
                       ;clojure function.
                       (pull-messages ib)
                      )
              (throw (RuntimeException. (str "Unknown control command: " command))))))
