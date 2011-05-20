(ns
    #^{:author "Eric Harris-Braun"
       :doc "A reference implementation of the ceptr platform"}
  anansi.core
  (:use [anansi.server :only [launch-server]]
        [anansi.ceptr]
        [anansi.user]
        [anansi.receptor.host]
        [anansi.receptor.scape]
        [anansi.receptor.user]
        [anansi.commands :only [execute]]
        [anansi.user :only [user-streams]]
        [anansi.server-constants :only [get-host]]
        [clojure.contrib.json :only [json-str]]
        )
  (:use compojure.core, ring.adapter.jetty)
  
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :as response]))

(defn get-user
  [username]
  (dosync
       (let [host (get-host)
             users (contents host :user-scape)
             user-address (s-> self->host-user host username) ;; creates or returns existing user receptor address
             user (get-receptor host user-address)]
         (--> key->set host users username user-address )
         (--> self->connect host user :web)
         (commute user-streams assoc username user)
)))

(defn view-form []
  (str "<html><head></head><body>"
       "<form method=\"post\">"
       "Command <input type=\"text\" name=\"cmd\"/>"
       "Data <input type=\"text\" name=\"data\"/>"
       "<input type=\"submit\"/>"
       "</form></body></html>"))
(defroutes main-routes
  (GET "/" [] (view-form))
  (POST "/" [cmd data username] (try
                                  (binding [*user-name* username]
                                    (get-user *user-name*)
                                    (let [result 
                                          (json-str (execute (if (or (= data "") (nil? data)) cmd (str cmd " " data))))]
                                      (dosync (commute user-streams dissoc username))
                                      result))
                                  (catch Exception e
                                    (.printStackTrace e *err*)
                                    (str {:status :error
                                          :result (str "exception raised: " e)}))))
  (route/files "/stuff" {:root "htdocs"})
  (route/not-found "Page not found"))
(def app
     (handler/site main-routes))

(defn -main    
  ([port]
     (do 
       (let [t (Thread. #(run-jetty app {:port 8080}))]
         (doto t .start))
       
       (launch-server port)))
  
  ([] (-main 3333)))

