(ns what.database
  (:require [clojure.java.io :as io]
            [honey.sql :as sql]
            [pod.babashka.go-sqlite3 :as sqlite]
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


(defn get-command-urls-records
  "Get URLs linked to a command."
  [command]
  (let [sql-sentence (sql/format {:select [:u.url]
                                  :from [[:command :c]]
                                  :join [[:url :u] [:= :c.command :u.command]]
                                  :where [:= :c.command command]})]
    (sqlite/query db sql-sentence)))


(defn add-command-url-record
  "Add an URL to a command."
  [command url]
  (let [sql-sentence (sql/format {:insert-into [:url]
                                  :columns [:url :command]
                                  :values [[url command]]})]
    (sqlite/execute! db sql-sentence)))


(defn remove-command-url-record
  "Remove an URL from a command."
  [command url]
  (let [sql-sentence (sql/format {:delete-from [:url]
                                  :where [:and [:= :command command] [:= :url url]]})]
    (sqlite/execute! db sql-sentence)))


(defn find-command-records
  "Find the command records which have a query-string on theirs command or description fields."
  [query-string fields]
  (let [valid-fields [:command :description :name]
        fields-elements (for
                         [field fields :when (some #{field} valid-fields)] [:like field (format "%%%s%%" query-string)])
        fields-sentence (cons :or fields-elements)
        sql-definition {:select [:command :description :name]
                        :from [:command]
                        :where fields-sentence}
        sql-sentence (sql/format sql-definition)]
    (sqlite/query db sql-sentence)))
