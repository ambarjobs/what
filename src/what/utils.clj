(ns what.utils
  (:require [clojure.string :as string]))


(defn prompted-input
  "Presents a prompt and reads a user input."
  [prompt]
  (print prompt)
  (flush)
  (read-line))


(defn process-string-list
  "Get an input string and return a list of elements coerced by the passed function."
  [string-list coerce-function]
  (map coerce-function (filter #(not= % "") (string/split (string/trim string-list) #"\s"))))
