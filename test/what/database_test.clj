(ns what.database-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [honey.sql :as sql]
            [pod.babashka.go-sqlite3 :as sqlite]
            [what.database :as db]
            [what.database-fixtures :as fixtures]))


(use-fixtures :once fixtures/database-creation-fixture)
(use-fixtures :each fixtures/database-population-fixture)


(deftest get-command-records-test
  (testing "Get all command records - Common case"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd0" :description "Description 0" :doc "man" :name nil}
              {:command "cmd1" :description "Description 1 (more)" :doc "--help" :name "Name"}
              {:command "cmd2" :description "Description 2" :doc "--another" :name nil}]
             (db/get-command-records)))))
  (testing "Get all command records - Empty database"
    (sqlite/execute! (str fixtures/temp-db-file) (sql/format {:delete-from :command}))
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= []
             (db/get-command-records))))
    (fixtures/reset-database)))


(deftest get-command-record-test
  (testing "Get the record corresponding specific command - Existing command"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= {:command "cmd1" :description "Description 1 (more)" :doc "--help" :name "Name"}
             (db/get-command-record "cmd1")))))
  (testing "Get the record corresponding specific command - Unexisting command"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= nil
             (db/get-command-record "unexisting-cmd")))))
  (testing "Get the record corresponding specific command - Empty database"
    (sqlite/execute! (str fixtures/temp-db-file) (sql/format {:delete-from :command}))
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= nil
             (db/get-command-record "cmd1"))))
    (fixtures/reset-database)))


(deftest insert-command-record-test
  (testing "Insert a new command into the database - All fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (let [new-command "new-command0"
            new-description "New description 0"
            new-doc "--new0"
            new-name "New name 0"]
        (db/insert-command-record new-command new-description new-doc new-name)
        (is (= {:command new-command :description new-description :doc new-doc :name new-name}
               (db/get-command-record new-command))))))
  (testing "Insert a new command into the database - Null name"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (let [new-command "new-command1"
            new-description "New description 1"
            new-doc "--new1"
            new-name nil]
        (db/insert-command-record new-command new-description new-doc new-name)
        (is (= {:command new-command :description new-description :doc new-doc :name new-name}
               (db/get-command-record new-command))))))
  (testing "Insert a new command into the database - Null doc"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (let [new-command "new-command2"
            new-description "New description 2"
            new-doc nil
            new-name "New name 2"]
        (db/insert-command-record new-command new-description new-doc new-name)
        (is (= {:command new-command :description new-description :doc new-doc :name new-name}
               (db/get-command-record new-command))))))
  (testing "Insert a new command into the database - Null command error"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (let [new-command nil
            new-description "New description 3"
            new-doc "--new3"
            new-name "New name 3"]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"NOT NULL constraint failed: command.command"
                              (db/insert-command-record new-command new-description new-doc new-name))))))
  (testing "Insert a new command into the database - Null description error"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (let [new-command "new-command4"
            new-description nil
            new-doc "--new4"
            new-name "New name 4"]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"NOT NULL constraint failed: command.description"
                              (db/insert-command-record new-command new-description new-doc new-name))))))
  (testing "Insert a new command into the database - Pre-existent command"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (let [new-command "new-command0"
            new-description "New description 5"
            new-doc "--new5"
            new-name "New name 5"]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"UNIQUE constraint failed: command.command"
                              (db/insert-command-record new-command new-description new-doc new-name)))))))


(deftest delete-command-record-test
  (testing "Delete a specific command record - Common case"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (let [result (db/delete-command-record "cmd1")]
        (is (= (:rows-affected result) 1))
        (is (= nil (db/get-command-record "cmd1")))))
    (fixtures/reset-database))
  (testing "Delete a specific command record - Unexisting command"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (let [result (db/delete-command-record "unexisting-cmd")]
        (is (= (:rows-affected result) 0))))))


(deftest update-command-record-test
  (let [orig-command "cmd1"
        orig-description "Description 1 (more)"
        orig-doc "--help"
        orig-name "name"]
    (testing "Update a command record - Obligatory field"
      (with-redefs [db/db (str fixtures/temp-db-file)]
        (let [result (db/update-command-record orig-command "New description" orig-doc orig-name)]
          (is (= (:rows-affected result) 1))
          (is (= {:command orig-command :description "New description" :doc orig-doc :name orig-name}
                 (db/get-command-record orig-command))))))
    (testing "Update a command record - Existing optional field to null"
      (with-redefs [db/db (str fixtures/temp-db-file)]
        (let [result (db/update-command-record orig-command orig-description orig-doc nil)]
          (is (= (:rows-affected result) 1))
          (is (= {:command orig-command :description orig-description :doc orig-doc :name nil}
                 (db/get-command-record orig-command)))))))
  (let [orig-command "cmd0"
        orig-description "Description 0"
        orig-doc "man"
        orig-name nil
        new-name "New name"]
    (testing "Update a command record - Not null optional field to null"
      (with-redefs [db/db (str fixtures/temp-db-file)]
        (let [result (db/update-command-record orig-command orig-description nil orig-name)]
          (is (= (:rows-affected result) 1))
          (is (= {:command orig-command :description orig-description :doc nil :name orig-name}
                 (db/get-command-record orig-command))))))
    (testing "Update a command record - Null optional field to null"
      (with-redefs [db/db (str fixtures/temp-db-file)]
        (let [result (db/update-command-record orig-command orig-description orig-doc nil)]
          (is (= (:rows-affected result) 1))
          (is (= {:command orig-command :description orig-description :doc orig-doc :name nil}
                 (db/get-command-record orig-command))))))
    (testing "Update a command record - Null optional field to not null"
      (with-redefs [db/db (str fixtures/temp-db-file)]
        (let [result (db/update-command-record orig-command orig-description orig-doc new-name)]
          (is (= (:rows-affected result) 1))
          (is (= {:command orig-command :description orig-description :doc orig-doc :name new-name}
                 (db/get-command-record orig-command))))))))


(deftest get-command-urls-test
  (testing "Get command URLs - Just one URL"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:url "https://test-url2"}]
             (db/get-command-urls "cmd2")))))
  (testing "Get command URLs - More than one URL"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:url "https://test-url0a"}
              {:url "https://test-url0b"}]
             (db/get-command-urls "cmd0")))))
  (testing "Get command URLs - No URL"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [] (db/get-command-urls "cmd1")))))
  (testing "Get command URLs - Unexisting command"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [] (db/get-command-urls "unexisting-cmd"))))))


(deftest find-command-records-test
  (testing "Find query string - Existing string in command complete - All fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd1" :description "Description 1 (more)" :name "Name"}]
             (db/find-command-records "cmd1" [:command :description :name])))))
  (testing "Find query string - Existing string in command partial - All fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd1" :description "Description 1 (more)" :name "Name"}]
             (db/find-command-records "md1" [:command :description :name])))))
  (testing "Find query string - Existing string in description complete - All fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd1" :description "Description 1 (more)" :name "Name"}]
             (db/find-command-records "Description 1 (more)" [:command :description :name])))))
  (testing "Find query string - Existing string in description partial - All fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd1" :description "Description 1 (more)" :name "Name"}]
             (db/find-command-records "more" [:command :description :name])))))
  (testing "Find query string - Existing string in name complete - All fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd1" :description "Description 1 (more)" :name "Name"}]
             (db/find-command-records "Name" [:command :description :name])))))
  (testing "Find query string - Existing string in name partial - All fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd1" :description "Description 1 (more)" :name "Name"}]
             (db/find-command-records "Nam" [:command :description :name])))))
  (testing "Find query string - Existing string complete - Some fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd1" :description "Description 1 (more)" :name "Name"}]
             (db/find-command-records "Description 1 (more)" [:description :name])))))
  (testing "Find query string - Existing string partial - Some fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd1" :description "Description 1 (more)" :name "Name"}]
             (db/find-command-records "more" [:description :name])))))
  (testing "Find query string - Existing string complete - One field"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd1" :description "Description 1 (more)" :name "Name"}]
             (db/find-command-records "Name" [:name])))))
  (testing "Find query string - Existing string partial - One field"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd1" :description "Description 1 (more)" :name "Name"}]
             (db/find-command-records "ame" [:name])))))
  (testing "Find query string - Existing string partial - No field"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [] (db/find-command-records "Description" [])))))
  (testing "Find query string - Unexisting string - All fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [] (db/find-command-records "unexisting" [:command :description :name])))))
  (testing "Find query string - Unexisting string - Some fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [] (db/find-command-records "unexisting" [:description :name])))))
  (testing "Find query string - Not found string - Some fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [] (db/find-command-records "cmd1" [:description :name])))))
  (testing "Find query string - Existing string in multiple records - All fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd0" :description "Description 0" :name nil}
              {:command "cmd1" :description "Description 1 (more)" :name "Name"}
              {:command "cmd2" :description "Description 2" :name nil}]
             (db/find-command-records "Description" [:command :description :name])))))
  (testing "Find query string - Existing string in multiple records - Some fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd0" :description "Description 0" :name nil}
              {:command "cmd1" :description "Description 1 (more)" :name "Name"}
              {:command "cmd2" :description "Description 2" :name nil}]
             (db/find-command-records "Description" [:description :name])))))
  (testing "Find query string - Existing string in multiple records - Additional invalid fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd0" :description "Description 0" :name nil}
              {:command "cmd1" :description "Description 1 (more)" :name "Name"}
              {:command "cmd2" :description "Description 2" :name nil}]
             (db/find-command-records "Description" [:command :description :name :invalid])))))
  (testing "Find query string - Not found existing string in multiple records - Some fields"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [] (db/find-command-records "Description" [:command :name])))))
  (testing "Find query string - Empty query string"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [{:command "cmd0" :description "Description 0" :name nil}
              {:command "cmd1" :description "Description 1 (more)" :name "Name"}
              {:command "cmd2" :description "Description 2" :name nil}]
             (db/find-command-records "" [:command :description :name])))))
  (testing "Find query string - Null query string"
    (with-redefs [db/db (str fixtures/temp-db-file)]
      (is (= [] (db/find-command-records nil [:command :description :name]))))))
