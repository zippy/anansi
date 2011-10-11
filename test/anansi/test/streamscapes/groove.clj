(ns anansi.test.streamscapes.groove
  (:use [anansi.streamscapes.groove] :reload)
  (:use [anansi.ceptr])
  (:use [midje.sweet])
  (:use [clojure.test]))

(facts "about groove receptors"
  (let [g (make-receptor groove-def nil {:attributes {:grammars {:streamscapes "some-grammar-spec" }}})]
    ;; atributes
    (receptor-state g false) => (contains {:grammars {:streamscapes "some-grammar-spec" } :fingerprint :anansi.streamscapes.groove.groove})
    ;; restore
    (=  (receptor-state g true) (receptor-state (receptor-restore (receptor-state g true) nil) true))  => true
    )
  )
