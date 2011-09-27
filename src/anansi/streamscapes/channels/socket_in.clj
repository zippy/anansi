(ns
  #^{:author "Eric Harris-Braun"
     :doc "Socket input receptor"}
  anansi.streamscapes.channels.socket-in
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel])
  (:use [clj-time.core :only [now]]))

(def socket-in-def (receptor-def "socket-in"))

(defn handle-message [_r message]
  "process socket input"
  (let [{from-ip :from to-ip :to msg :message} message
        id (str from-ip "-" (now))
        ss (parent-of (parent-of _r))
        ids (get-scape ss :id)
        da (s-> address->resolve ids id)]
    (if (empty? da)
      (let [from-id (do-identify ss {:identifiers {:ip from-ip}} false)
            to-id (do-identify ss {:identifiers {:ip to-ip}} false)]
        (--> stream->receive _r (parent-of _r)
             {:id id
              :to to-id
              :from from-id
              :envelope {:from "ip/address" :message "text/plain"}
              :content {:from from-ip
                        :to to-ip
                        :message msg}}))
      (first da))))

(signal controller receive [_r _f msg]
        (handle-message _r msg))
