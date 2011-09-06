(ns
  #^{:author "Eric Harris-Braun"
     :doc "IRC Bridge receptor"}
  anansi.streamscapes.channels.irc-bridge-in
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        [anansi.streamscapes.streamscapes]
        [anansi.streamscapes.channel])
  (:use [clj-time.core :only [now]]))

(defmethod manifest :irc-bridge-in [_r {}]
           {})
(defmethod state :irc-bridge-in [_r full?]
           (state-convert _r full?))
(defmethod restore :irc-bridge-in [state parent]
           (let [r (do-restore state parent)]
             r))

(defn handle-message [_r msg]
  "process an IRC message"
  (let [
        [_1 _2 from-address command _3 params] (re-find #"^(:([^ ]*) )*([a-zA-Z]*)( (.*))*$" msg)
        id (str from-address "-" (now))
        ss (parent-of (parent-of _r))
        ids (get-scape ss :id)
        da (s-> address->resolve ids id)]
    (if (and (empty? da) (= command "PRIVMSG"))
      (let [[_ irc-to message] (re-find #"([^ ]*) :(.*)" params)
            to-type (if (= \# (first irc-to)) "irc/channel" "irc/user")
            [_ nick user host] (re-find #"(.*)!(.*)@(.*)" from-address)
            from-id (do-identify ss {:identifiers {:irc nick}} false)
            to-id (do-identify ss {:identifiers {:irc irc-to}} false)]
        (--> stream->receive _r (parent-of _r)
             {:id id
              :to to-id
              :from from-id
              :envelope {:from "irc/from" :cmd "irc/command" :to to-type :message "text/plain"}
              :content {:from from-address
                        :cmd command
                        :to irc-to
                        :message message}}))
      (first da))))

(signal controller receive [_r _f msg]
        (handle-message _r msg))
