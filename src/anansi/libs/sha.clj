(ns anansi.libs.sha
  (:use clojure.contrib.str-utils)
  (:import [java.security MessageDigest]))

(defn sha
  "Generates a SHA-256 hash of the given input plaintext."
  [input]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (. md update (.getBytes input))
    (let [digest (.digest md)]
      (str-join "" (map #(Integer/toHexString (bit-and % 0xff)) digest)))))
