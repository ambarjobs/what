(ns what.database-fixtures
  (:require [honey.sql :as sql]
            [pod.babashka.go-sqlite3 :as sqlite])
  (:import (java.io File)))


(def temp-db-file (File/createTempFile "test" ".sqlite3"))


(defn populate-database
  "Populate tes database with default records."
  []
  (let [populate-command-sql (sql/format {:insert-into [:command]
                                          :columns [:command :description :doc :name]
                                          :values [["cmd000" "Description 0" "man" nil]
                                                   ["cmd1" "Description 1 (more)" "--help" "Name"]
                                                   ["cmd2" "Description 2" "--another" nil]]})
        populate-url-sql (sql/format {:insert-into [:url]
                                      :columns [:url :command]
                                      :values [["https://test-url0a" "cmd000"]
                                               ["https://test-url0b" "cmd000"]
                                               ["https://test-url2" "cmd2"]]})]
    (sqlite/execute! (str temp-db-file) populate-command-sql)
    (sqlite/execute! (str temp-db-file) populate-url-sql)))


(defn clear-database
  "Clear test database records."
  []
  (doseq [table [:command :url]]
    (sqlite/execute! (str temp-db-file) (sql/format {:delete-from table}))))


(defn reset-database
  "Reset test database to it's original default values."
  []
  (clear-database)
  (populate-database))


(defn database-creation-fixture
  [test-function]
  (let [create-command-table-sql (sql/format {:create-table :command
                                              :with-columns [[:command [:varchar 255] :unique [:not nil]]
                                                             [:description [:varchar 255] [:not nil]]
                                                             [:doc [:varchar 255]]
                                                             [:name [:varchar 255]]]})
        create-url-table-sql (sql/format {:create-table :url
                                          :with-columns [[:url [:varchar 255] [:not nil]]
                                                         [:command [:varchar 255] [:not nil]]]})]
    (sqlite/execute! (str temp-db-file) create-command-table-sql)
    (sqlite/execute! (str temp-db-file) create-url-table-sql)
    (test-function)
    (.delete temp-db-file)))


(defn database-population-fixture
  [test-function]
  (populate-database)
  (test-function)
  (clear-database))
