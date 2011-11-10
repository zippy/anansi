(ns
  #^{:author "Eric Harris-Braun"
     :doc "twitter Bridge receptor"}
  anansi.streamscapes.channels.twitter-bridge-in
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel]
        [anansi.util :only [standard-date-string]])
  (:use [clj-time.core :only [now]]))

(def twitter-bridge-in-def (receptor-def "twitter-bridge-in"))

(defn handle-message [_r msg]
  "process a twitter message"
  (let [{id :id_str text :text from-address :from_user} msg
        ss (parent-of (parent-of _r))
        ids (get-scape ss :id)
        da (s-> address->resolve ids id)]
    (if (empty? da)
      (let [twitter-to "_twp_"
            from-id (do-identify ss {:identifiers {:twitter-address (str "@" from-address)} :attributes {:twitter-avatar (:profile_image_url msg)}}  false)
            to-id (do-identify ss {:identifiers {:twitter-address twitter-to}} false)]

        (--> stream->receive _r (parent-of _r)
             {:id id
              :to to-id
              :sent (standard-date-string (:created_at msg))
              :from from-id
              :envelope {:from "twitter/screen_name" :message "text/plain"}
              :content {:from from-address
                        :message text}}))
      (first da))))

(signal controller receive [_r _f msg]
        (handle-message _r msg))
