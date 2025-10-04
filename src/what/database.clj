(ns what.database
  (:require [pod.babashka.go-sqlite3 :as sqlite]
            ;; [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as helper]))

(def db "/home/armando/Desenv/Clojure/Babashka/what/db/what.sqlite3")

(defn get-command-records
  "Get all command records from database."
  []
  (let [sql ["select command, description, doc, name from command order by command.command"]]
    (sqlite/query db sql)))
