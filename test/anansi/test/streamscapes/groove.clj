(ns anansi.test.streamscapes.groove
  (:use [anansi.streamscapes.groove] :reload)
  (:use [anansi.ceptr])
  (:use [midje.sweet])
  (:use [clojure.test]))

(facts "about groove receptors"
  (let [g (make-receptor groove-def nil {:attributes {:name :subject-body-message}})]
    ;; atributes
    (receptor-state g false) => (contains {:name :subject-body-message
                                           :grammar {:subject "text/plain" :body "text/html"}
                                           :preview :subject
                                           :carriers {:email {:encoding nil
                                                              :actions {:create true :reply true}}
                                                      :streamscapes {:encoding nil
                                                                     :actions {:create true :reply true}}}
                                           :fingerprint :anansi.streamscapes.groove.groove})
    ;; restore
    (=  (receptor-state g true) (receptor-state (receptor-restore (receptor-state g true) nil) true))  => true
    ;; signals
    
    )
  )

(facts "about grammar-match?"
  (grammar-match? {:subject "text/plain" :body "text/html"} {:subject "text/plain" :body "text/plain"} {:subject "Hi there" :body "yo!"}) => true
  (grammar-match? {:subject "text/plain" :body "text/html"} {:subject "text/plain"} {:subject "Hi there"}) => false
  (grammar-match? {:message {"text" [#"yo!"]}} {:message "text/plain"} {:message "hey yo!"}) => true
  (grammar-match? {:message {"text" [#"yo!"]}} {:message "text/plain"} {:message "boink"}) => false
  (grammar-match? {:message {"text" [#"yo!"]}} {:message "img/jpg"} {:message "yo!"}) => false
  (let [punkmoney (-> compository :punkmoney :matchers)]
    (grammar-match? (:subject-body-message punkmoney)
                    {:subject "text/plain" :body "text/plain"}
                    {:subject "Punkmoney Promise" :body "I promise to pay eric@example.com, on demand, some squids. Expires in 1 year."}) =>
                    {:promised-to "eric@example.com" :promised-item "some squids" :expiration "in 1 year"}
    (grammar-match? (:subject-body-message punkmoney)
                    {:subject "text/plain" :body "text/plain"}
                    {:subject "Punkmoney" :body "I promise to pay eric@example.com, on demand, some squids. Expires in 1 year."}) =>
                    false
    (grammar-match? (:simple-message punkmoney)
                    {:message "text/plain"}
                    {:message "@artbrock I promise to pay, on demand, some squids. Expires in 1 year. #punkmoney"}) =>
                    {:promised-to "@artbrock" :promised-item "some squids" :expiration "in 1 year"}
                    )
  

;;  (grammar-match? {:subject "text/plain" :body [#"yo!"]} {:subject "text/plain" :body "text/plain"} {:subject "Hi there" :body "yo!"}) => true
  )
