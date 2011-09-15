(ns
  #^{:author "Eric Harris-Braun"
     :doc "http host interface receptor"}
  anansi.receptor.host-interface.http
  (:use [anansi.ceptr]
        [anansi.receptor.user]
        [anansi.receptor.scape]
        [anansi.receptor.host]
        [anansi.receptor.host-interface.commands :only [execute]])
  (:use [lamina.core]
        [aleph.http]
        [aleph.core]
        [aleph.formats]
        [net.cgrand.moustache])
  )

(defmethod manifest :http-host-interface [_r {}]
           {:server nil})
(defmethod state :http-host-interface [_r full?]
           (state-convert _r full?))
(defmethod restore :http-host-interface [state parent]
           (let [r (do-restore state parent)]
             r))

(defn welcome [request]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "Welcome to the Anansi sever."})

(defn make-http-handler [host _r]
  (app
   [""] {:get welcome}
   ["api"] {:post (fn [request]
                    (let [{command :cmd params :params} (decode-json (:body request))
                          result (execute host _r command params)]
                      (try
                        {:status 200
                         :content-type "application/json"
                         :body result}
                        (catch Exception e
                           (.printStackTrace e *err*)
                           {:status 500
                            :content-type "text/plain"
                            :body (.getMessage e)})
                        )))})
  )

(signal interface start [_r _f {port :port}]
        (if (contents _r :server)
          (throw (RuntimeException. "Server already started."))
          (rsync _r
                 (let [host (parent-of _r)]
                   (set-content _r :server (start-http-server (wrap-ring-handler (make-http-handler host _r)) {:port port}))))))

(signal interface stop [_r _f]
        (let [server (contents _r :server)]
          (if (not server)
            (throw (RuntimeException. "Server not started."))
            (rsync _r
                   (server) ;; an aleph server is a function which
                   ;; when called stops it.  Odd!
                   (set-content _r :server nil)))))
