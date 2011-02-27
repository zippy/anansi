(ns anansi.test.core
  (:use [ anansi.test.commands] :reload)
  (:use [ anansi.test.receptor] :reload)
  (:use [anansi.core] :reload)
  (:use [clojure.test]))

;; Until we learn better: this file should :use all the ns's with
;; anansi tests in them.  Then all tests can be run in slime/swank by compiling
;; this file and running (clojure.test/run-tests),  or, since that
;; command seems to include running other namespaces, list them
;; explicitly like so:
;; (clojure.test/run-tests 'anansi.test.core 'anansi.test.commands 'anansi.test.receptor )

;; This is mostly useful for quickly re-running the whole suite, eg
;; for a big refactor.  For individual test or test files, use clojure-test-mode.

