(ns what.database
  (:require [clojure.java.io :as io]
            [pod.babashka.go-sqlite3 :as sqlite]
            [honey.sql :as sql]
            [what.config :refer [config-data]]))


(def db (->>
  (:db-file config-data)
  io/resource
  io/as-file
  str))


(defn get-command-records
  "Get all command records from database."
  []
  (let [sql-sentence (sql/format {:select [:command :description :doc :name]
                                  :from [:command]
                                  :order-by [:command]})]
    (sqlite/query db sql-sentence)))


(defn get-command-record
  "Get the record corresponding to a specific command."
  [command]
  (let [sql-sentence (sql/format {:select [:command :description :doc :name]
                                  :from [:command]
                                  :where [:= :command command]})]
    (first (sqlite/query db sql-sentence))))


(defn insert-command-record
  "Add command record to the database."
  [command description doc name]
  (let [sql-sentence (sql/format {:insert-into [:command]
                                  :columns [:command :description :doc :name]
                                  :values [[command description doc name]]})]
    (sqlite/execute! db sql-sentence)))


(defn delete-command-record
  "Remove a command record from the database."
  [command]
  (let [sql-sentence (sql/format {:delete-from [:command]
                                  :where [:= :command command]})]
    (sqlite/execute! db sql-sentence)))


(defn update-command-record
  "Update a command record on the database."
  [command description doc name]
  (let [sql-sentence (sql/format {:update [:command]
                                  :set {:description description
                                        :doc doc
                                        :name name}
                                  :where [:= :command command]})]
    (sqlite/execute! db sql-sentence)))
