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
        [ring.middleware.file]
        [ring.middleware.params]
        [ring.util.response :only [file-response]]
        [net.cgrand.moustache]
        [clojure.contrib.duck-streams :only [pwd]]
        [clojure.string :only [trim]]
        [clojure.walk :only [keywordize-keys]])
  )

(let [attributes #{:auto-start}]

  (defmethod manifest :http-host-interface [_r params]
             (into {:server nil} (map (fn [a] [a (a params)]) attributes)))
  (defmethod state :http-host-interface [_r full?]
             (merge (state-convert _r full?)
                    (into {} (map (fn [a] [a (contents _r a)]) attributes))))
  (defmethod restore :http-host-interface [state parent]
             (let [r (do-restore state parent)]
               (doall (map (fn [a] (restore-content r a (a state))) attributes))
               r))
  )
(declare interface->start)
(defmethod animate :http-host-interface [_r]
           (let [auto-start (contents _r :auto-start)]
             (if auto-start (s-> interface->start _r auto-start))))

(defn welcome [request]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "Welcome to the Anansi sever."})

(def file-not-found 
  {:status 404
   :body "Not Found"})

(defn server-error [e]
  (do
    (println "server error: " (.getMessage e))
    (.printStackTrace e *err*)
    {:status 200
     :content-type "application/json"
     :body {:status :error :result (.getMessage e)} })
  )

(defn file-handler [request]
  (try
    (if-let [file (file-response (str "public" (:uri request)))]
      file
      file-not-found
      )
    (catch Exception e
      (server-error e))
    ))

(defn make-http-handler [host _r]
  (app
   ["api"] {:post (fn [request]
                    (try
                      (let [b (trim (bytes->string (:body request)))
                            {command :cmd params :params} (clojure.contrib.json/read-json b)]
                        (println (str "POST REQUEST: from " (:remote-addr request) " for: " b))
                        {:status 200
                         :content-type "application/json"
                         :body (execute host _r command params)
                         })
                      (catch Exception e
                        (server-error e))))
            :get (wrap-params (fn [request]
                                (try
                                  {:status 500
                                   :content-type "text/plain"
                                   :body "post json reqests only"}
                                  (catch Exception e
                                    (server-error e)))
                                ))}

   [&] file-handler))

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
