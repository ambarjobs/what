(ns what.database
  (:require [pod.babashka.go-sqlite3 :as sqlite]
            ;; [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as helper]))

(def db "db/what.sqlite3")

(defn get-commands
  []
  (let [sql ["select command, description from command order by command.command"]]
    (sqlite/query db sql)))
