(ns anansi.server
  (:use [anansi.receptor :only [create-membrane]])
  )

(def *server-receptor* (create-membrane))
