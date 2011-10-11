(defproject anansi "0.0.1"
  :description "a reference server implementation of the ceptr platform"
  :repositories {"java.net" "http://download.java.net/maven/2"
                 "clojars" "http://clojars.org/repo"}
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [aleph "0.2.0-beta2"]
                 [lamina "0.4.0-beta2"]
                 [org.jboss.netty/netty "3.2.4.Final"]
                 [clj-http "0.1.3"]
                 [gloss "0.2.0-beta2"]
                 [user-agent-utils "1.2.3"]
                 [potemkin "0.1.0"]

                 [net.cgrand/moustache "1.0.0"]
                 [compojure "0.6.3"]
                 [ring "0.3.7"]
                 [clj-time "0.3.0"]
                 [javax.mail/mail "1.4.4"
                  :exclusions [javax.activation/activation]]
                 ]
  :dev-dependencies [[swank-clojure "1.2.1"]
                     [midje "1.3-alpha1"]
                     [autodoc "0.7.1" :exclusions [org.clojure/clojure-contrib 
                                                    org.clojure/clojure]]
;;                      [org.clojars.mjul/lein-cuke "1.1.0"]
                     ]
  :autodoc {:name "Anansi", :page-title "Anansi Docs"
            :description ""
            :copyright "Documentation: CC; Code: Eclipse Public License"}
  :main anansi.core)
