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

(declare interface->start)
(def http-def (receptor-def "http"
                            (attributes :auto-start)
                            (manifest [_r attrs]
                                      (merge {:server nil} (extract-receptor-attributes-from-map _r attrs)))
                            (animate [_r]
                                     (let [auto-start (contents _r :auto-start)]
                                       (if auto-start (s-> interface->start _r auto-start)))
                                     )))

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

(defn mime-type [f]
  (condp =  (last (clojure.string/split (.getName f) #"\."))
      "css" "text/css"
      "js" "application/javascript"
      "html" "text/html"
      "text/plain")
  )

(defn file-handler [request]
  (try
    (if-let [file (file-response (str "public" (:uri request)))
             ]
      (assoc file :headers {"content-type" (mime-type (:body file))})
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
                         :headers {"content-type" "application/json"
                                   "Access-Control-Allow-Origin" "*"}
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
