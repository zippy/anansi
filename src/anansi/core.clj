(ns
    #^{:author "Eric Harris-Braun"
       :doc "A reference implementation of the ceptr platform"}
  anansi.core
  (:use [anansi.server :only [launch-server]]
        [anansi.commands :only [execute]]
        [clojure.contrib.json :only [json-str]]
        )
  (:use compojure.core, ring.adapter.jetty)
  
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))


(defn view-form []
  (str "<html><head></head><body>"
       "<form method=\"post\">"
       "Command <input type=\"text\" name=\"cmd\"/>"
       "Data <input type=\"text\" name=\"data\"/>"
       "<input type=\"submit\"/>"
       "</form></body></html>"))
       
(defroutes main-routes
  (GET "/stuff/:file.:ext" [file ext] (try (slurp (str file "." ext)) (catch Exception e (str {:status :error
                                     :result (str "exception raised: " e)}))))
  (GET "/" [] (view-form))
  (POST "/" [cmd data] (try  (json-str (execute (str cmd " " data)))
                             ;(pprint-json (execute (str cmd data)))
                             (catch Exception e
                               (.printStackTrace e *err*)
                               (str {:status :error
                                     :result (str "exception raised: " e)}))
                             
                             ))
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

