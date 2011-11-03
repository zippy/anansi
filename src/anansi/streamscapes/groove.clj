(ns
  #^{:author "Eric Harris-Braun"
     :doc "Groove receptor"}
  anansi.streamscapes.groove
  (:use [anansi.ceptr]
        [anansi.receptor.scape]
        ))
(declare compository)
(def groove-def (receptor-def "groove" (attributes :name :grammar :preview :carriers)
                              (manifest [_r {{name :name} :attributes}]
                                        {:name name
                                         :grammar (-> compository name :grammar)
                                         :preview (-> compository name :preview)
                                         :carriers (-> compository name :carriers)
                                         })))

(def compository
     {:subject-body-message {:grammar {:subject "text/plain" :body "text/html"}
                             :preview :subject
                             :carriers {:email {:encoding nil
                                                :actions {:create true :reply true}}
                                        :streamscapes {:encoding nil
                                                       :actions {:create true :reply true}}}
                             }
      :simple-message {:grammar {:message "text/plain"}
                       :preview :message
                       :carriers {:twitter {:encoding nil}
                                  :irc {:encoding nil
                                        :actions {:create true :reply true}}
                                  :streamscapes {:encoding nil
                                                 :actions {:create true :reply true}}}}
      :punkmoney {:actions {:create true :reply true}
                  :grammar {:promised-to "address" :promised-item "text/plain" :expiration "text/plain"}
                  :preview ["I promise to pay " [:promised-to] ", on demand, " [:promised-item] ". Expires " [:expiration]]
                  :carriers {:twitter {:encoding :simple-message}
                             :streamscapes {:encoding nil :actions {:create true}}
                             :email {:encoding :subject-body-message :actions {:create true}}}
                  :matchers {:subject-body-message
                             {:subject {"text" [#"Punkmoney Promise"]}
                              :body {"text"
                                     [#"I promise( to pay)* ([^\\W,]+)(, on demand)*,* ([^.]+)\. Expires (.*)\."
                                      {:promised-to 2
                                       :promised-item 4
                                       :expiration 5}]}}
                             :simple-message
                             {:message {"text"
                                        [#"^([^\\W]+) I promise( to pay)*(, on demand)*,* ([^.]+)\. Expires (.*)\.\W+#punkmoney"
                                         {:promised-to 1
                                          :promised-item 4
                                          :expiration 5}
                                         ]}}}}})

(defn grammar-match?
  "returns whether or not a grammar matches the carrier and content of a particular signal"
  [grammar carrier content]
  (if (nil? grammar)
    false
    (let [matches 
          (into [] (map (fn [[k sub-grammar]]
                          (if (string? sub-grammar)
                         
                            ;; if the grammar doesn't care about the content of the signal,
                            ;; then we have a match if just the keys in the carrier and the
                            ;; grammar match
                            (contains? carrier k)
                           
                            ;; othewise we have have make sure the 
                            ;; content matches. 
                            ;; TODO: for now this assumes only one
                            ;; sub-grammar specification, "text" for
                            ;; which the pattern matching is regex.  This
                            ;; needs to be generalized
                            (let [[re field-match-map] (sub-grammar "text")]
                              (if (and (not (nil? re))
                                       (let [content-type (k carrier)] (and (not (nil? content-type)) (re-find #"^text" content-type) )))
                                (let [match (re-find re (k content))]
                                  (if match
                                    (if (nil? field-match-map)
                                      true
                                      (into {} (map (fn [[field idx]] [field (match idx)]) field-match-map)))
                                    false))
                                false
                                ))
                            ))
                        grammar))]
      (if (every? identity matches)
        (let [m (into [] (filter #(map? %) matches))]
          (if (empty? m) true
              (apply merge m)))
        false))))

;;(when-carried-by :hash :subject-body-message {:match-fun (fn [hash params] (every? #(contains? hash %) params)) :match-params [:subject :body]})
;;(def carrier-grammars     {:hash {:subject-body-message {:subject "text/plain" :body "text/html"}}})
(comment def compository
     {:simple-message {:actions {:streamscapes [:create :reply]
                                 :irc [:create :reply]}
                       :grammars {:streamscapes {:message "text/plain"}
                                  :twitter {:text "text/plain"}
                                  :irc {:message "text/plain"}}}
      :subject-body-message {:actions {:streamscapes {:create true :reply true}
                                       :email [:create :reply]}
                             :grammars {:streamscapes {:subject "text/plain" :body "text/html"}
                                        :email {:subject "text/plain" :body "text/html"}}}
      :punkmoney {:actions {:streamscapes [:create]
                            :email {:create true :reply true}}
                  :grammars
                  {:streamscapes {:promised-good "text/plain"
                                  :expiration "text/plain"}
                   :email {:subject {"text" ["Punkmoney Promise"]}
                           :body {"text"
                                  ["I promise to pay (.*), on demand, ([^.]+)\\. Expires (.*)\\."
                                   {:payee 1
                                    :promised-good 2
                                    :expiration 3}
                                   ]}}
                   :twitter {:text {"text"
                                    ["^(@[^\\W]+) I promise to pay, on demand, ([^.]+)\\. Expires (.*)\\. #punkmoney"
                                     {:payee 1
                                      :promised-good 2
                                      :expiration 3}
                                     ]}}}}
      :poll {:actions {:streamscapes [:create]}
             :grammars {:streamscapes {:poll-name "text/plain"
                                       :options "enumeration/yes,no,abstain"}}}
      :bookmark {:actions {:streamscapes [:create]}
                 :grammars {:streamscapes {:url "text/plain"
                                           :subject "text/html"}}}
      :lazyweb-thanks {:actions {:streamscapes [:create]}
                       :grammars
                       {:streamscapes {:thankee "text/plain"
                                       :expiration "text/plain"}
                        :twitter {:text {"text"
                                         ["#lazyweb thanks (@[^\\W]+)"
                                          {:thankee 1}
                                          ]}}}}
      
      }
     )
