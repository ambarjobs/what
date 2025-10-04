(ns what.actions-handling
  (:require
   [clojure.string :as string]
   [what.database :as db]))



(defn list-commands
  [_]
  (println (string/join "\n" (map #(:command %) (db/get-commands)))))
