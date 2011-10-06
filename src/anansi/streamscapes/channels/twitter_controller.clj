(ns
  #^{:author "Eric Harris-Braun"
     :doc "twitter Controller receptor"}
  anansi.streamscapes.channels.twitter-controller
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]
        [anansi.streamscapes.channels.twitter-bridge-in :only [handle-message]]
        )
  (:use [aleph.http]
        [aleph.formats]
        [lamina.core])
)

(def twitter-controller-def (receptor-def "twitter-controller"
                                          (attributes :screen-name)))

(signal channel control [_r _f control-params]
        (let [{command :command params :params} control-params]
          (condp = command
              :check (let [parent-channel (parent-of _r)
                          [in-bridge-address receive-signal] (get-receiver-bridge parent-channel)
                           ib (get-receptor parent-channel in-bridge-address)
                           result 
                           (comment sync-http-request {:method :get,
                                                      :url (str "https://api.twitter.com/1/statuses/user_timeline.json?screen_name=" (contents _r :screen-name))
                                                      }
                                                     10000)]
                       (handle-message ib {:id_str "121470088258916352" :text "Some short tweet" :user {:screen_name (contents _r :screen-name)}})
                       ;THIS IS WAY CHEATING!!  The controller needs
                       ;to send this as a signal, not call it as a
                       ;clojure function.
;                       (pull-messages ib)
                      )
              (throw (RuntimeException. (str "Unknown control command: " command))))))
