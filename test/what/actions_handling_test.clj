(ns what.actions-handling-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [what.actions-handling :as actions]
            [what.database :as db]
            [what.database-fixtures :as fixtures]))


(use-fixtures :once fixtures/database-creation-fixture)
(use-fixtures :each fixtures/database-population-fixture)


(deftest list-commands-test
  (testing "List all commands - Common case"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= "cmd000:  Description 0\n  cmd1:  [Name] Description 1 (more)\n  cmd2:  Description 2\n"
             (with-out-str (actions/list-commands nil))))))
  (testing "List all commands - Empty database"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (fixtures/clear-database)
      (is (= ""
             (with-out-str (actions/list-commands nil)))))))

(deftest show-command-info-test
  (testing "Show information of specific command - Provided command name"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= "\ncmd1:  [Name] Description 1 (more)\n"
             (with-out-str (actions/show-command-info {:opts {:command "cmd1"}}))))))
  (testing "Show information of specific command - No command name provided initially"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= "Digite o nome do comando: \ncmd1:  [Name] Description 1 (more)\n"
             (with-out-str (with-in-str "cmd1" (actions/show-command-info nil)))))))
  (testing "Show information of specific command - No command name provided after prompt"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= "Digite o nome do comando: \nComando [] não encontrado na base de dados.\n"
             (with-out-str (with-in-str "" (actions/show-command-info nil)))))))
  (testing "Show information of specific command - Unexistent command name"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= "\nComando [inexistent-cmd] não encontrado na base de dados.\n"
             (with-out-str (actions/show-command-info {:opts {:command "inexistent-cmd"}})))))))


(deftest add-command-test
  (testing "Add a new command to the database - All fields provided"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Descrição do comando: "
                  "Help do comando (man (default) | --help | --outra-opção): "
                  "Nome do utilitário (se diferente do nome do comando): \n"
                  "O comando [new-cmd] foi adicionado com sucesso.\n")
             (with-out-str
               (with-in-str "Description new\n--new\nNew name" (actions/add-command {:opts {:command "new-cmd"}})))))
      (is (= {:command "new-cmd" :description "Description new" :doc "--new" :name "New name"}
             (db/get-command-record "new-cmd")))
      (fixtures/reset-database)))
  (testing "Add a new command to the database - Optional fields with default values"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Descrição do comando: "
                  "Help do comando (man (default) | --help | --outra-opção): "
                  "Nome do utilitário (se diferente do nome do comando): \n"
                  "O comando [new-cmd] foi adicionado com sucesso.\n")
             (with-out-str
               (with-in-str "Description new\n\n" (actions/add-command {:opts {:command "new-cmd"}})))))
      (is (= {:command "new-cmd" :description "Description new" :doc "man" :name nil}
             (db/get-command-record "new-cmd")))
      (fixtures/reset-database)))
  (testing "Add a new command to the database - No command name provided initially"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite o nome do comando: "
                  "Descrição do comando: "
                  "Help do comando (man (default) | --help | --outra-opção): "
                  "Nome do utilitário (se diferente do nome do comando): \n"
                  "O comando [new-cmd] foi adicionado com sucesso.\n")
             (with-out-str
               (with-in-str "new-cmd\nDescription new\n--new\nNew name" (actions/add-command nil)))))
      (is (= {:command "new-cmd" :description "Description new" :doc "--new" :name "New name"}
             (db/get-command-record "new-cmd")))
      (fixtures/reset-database)))
  (testing "Add a new command to the database - No command name provided even after prompt"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite o nome do comando: \n"
                  "O nome do comando deve ser fornecido.\n")
             (with-out-str
               (with-in-str "\n\nDescription new\n--new\nNew name" (actions/add-command nil)))))
      (is (= nil (db/get-command-record "new-cmd")))
      (fixtures/reset-database)))
  (testing "Add a new command to the database - Pre-existent command"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Descrição do comando: "
                  "Help do comando (man (default) | --help | --outra-opção): "
                  "Nome do utilitário (se diferente do nome do comando): \n"
                  "O comando [cmd000] já existe na base de dados.\n")
             (with-out-str
               (with-in-str "Description new\n--new\nNew name" (actions/add-command {:opts {:command "cmd000"}})))))
      (is (= {:command "cmd000" :description "Description 0" :doc "man" :name nil}
             (db/get-command-record "cmd000")))
      (fixtures/reset-database))))


(deftest rm-command-test
  (testing "Remove a command from database - Existing command - Lower case confirmation"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Deseja realmente REMOVER o comando [cmd1]? (s/N): "
                  "\nComando [cmd1] removido com sucesso.\n")
             (with-out-str
               (with-in-str "s" (actions/rm-command {:opts {:command "cmd1"}})))))
      (is (= nil (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Remove a command from database - Existing command - Upper case confirmation"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Deseja realmente REMOVER o comando [cmd1]? (s/N): "
                  "\nComando [cmd1] removido com sucesso.\n")
             (with-out-str
               (with-in-str "S" (actions/rm-command {:opts {:command "cmd1"}})))))
      (is (= nil (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Remove a command from database - Existing command - Explicitly not confirmed with `n`"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Deseja realmente REMOVER o comando [cmd1]? (s/N): "
                  "\nComando [cmd1] NÃO removido.\n")
             (with-out-str
               (with-in-str "n" (actions/rm-command {:opts {:command "cmd1"}})))))
      (is (= {:command "cmd1" :description "Description 1 (more)" :doc "--help" :name "Name"}
             (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Remove a command from database - Existing command - Not confirmed with `<Enter>`"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Deseja realmente REMOVER o comando [cmd1]? (s/N): "
                  "\nComando [cmd1] NÃO removido.\n")
             (with-out-str
               (with-in-str "\n" (actions/rm-command {:opts {:command "cmd1"}})))))
      (is (= {:command "cmd1" :description "Description 1 (more)" :doc "--help" :name "Name"}
             (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Remove a command from database - No command name provided initially"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite o nome do comando: "
                  "Deseja realmente REMOVER o comando [cmd1]? (s/N): "
                  "\nComando [cmd1] removido com sucesso.\n")
             (with-out-str
               (with-in-str "cmd1\ns" (actions/rm-command nil)))))
      (is (= nil (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Remove a command from database - No command name provided even after prompt"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite o nome do comando: "
                  "Deseja realmente REMOVER o comando []? (s/N): "
                  "\nO comando [] não existe na base de dados.\n")
             (with-out-str
               (with-in-str "\ns" (actions/rm-command nil)))))
      (fixtures/reset-database)))
  (testing "Remove a command from database - Inexisting command"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Deseja realmente REMOVER o comando [inexisting-cmd]? (s/N): "
                  "\nO comando [inexisting-cmd] não existe na base de dados.\n")
             (with-out-str
               (with-in-str "s" (actions/rm-command {:opts {:command "inexisting-cmd"}})))))
      (fixtures/reset-database))))


(deftest upd-command-test
  (testing "Update a command on database - Existing command - New values in all fields"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite os novos valores, ou <Enter> para manter os valores atuais (mostrados entre colchetes).\n\n"
                  "Descrição do comando \n  [Description 1 (more)]: "
                  "Help do comando (man (default) | --help | --outra-opção) \n  [--help]: "
                  "Nome do utilitário (<Enter> para manter o nome ou :vazio para nome nulo) \n  [Name]: "
                  "\nO comando [cmd1] foi atualizado com sucesso.\n")
             (with-out-str
               (with-in-str "New description\n--new\nNew name" (actions/upd-command {:opts {:command "cmd1"}})))))
      (is (= {:command "cmd1" :description "New description" :doc "--new" :name "New name"}
             (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Update a command on database - Existing command - Same values explicitly for all fields"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite os novos valores, ou <Enter> para manter os valores atuais (mostrados entre colchetes).\n\n"
                  "Descrição do comando \n  [Description 1 (more)]: "
                  "Help do comando (man (default) | --help | --outra-opção) \n  [--help]: "
                  "Nome do utilitário (<Enter> para manter o nome ou :vazio para nome nulo) \n  [Name]: "
                  "\nO comando [cmd1] foi atualizado com sucesso.\n")
             (with-out-str
               (with-in-str "Description 1 (more)\n--help\nName" (actions/upd-command {:opts {:command "cmd1"}})))))
      (is (= {:command "cmd1" :description "Description 1 (more)" :doc "--help" :name "Name"}
             (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Update a command on database - Existing command - Same values using default values for all fields"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite os novos valores, ou <Enter> para manter os valores atuais (mostrados entre colchetes).\n\n"
                  "Descrição do comando \n  [Description 1 (more)]: "
                  "Help do comando (man (default) | --help | --outra-opção) \n  [--help]: "
                  "Nome do utilitário (<Enter> para manter o nome ou :vazio para nome nulo) \n  [Name]: "
                  "\nO comando [cmd1] foi atualizado com sucesso.\n")
             (with-out-str
               (with-in-str "\n\n\n" (actions/upd-command {:opts {:command "cmd1"}})))))
      (is (= {:command "cmd1" :description "Description 1 (more)" :doc "--help" :name "Name"}
             (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Update a command on database - Existing command - Using default values for some fields"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite os novos valores, ou <Enter> para manter os valores atuais (mostrados entre colchetes).\n\n"
                  "Descrição do comando \n  [Description 1 (more)]: "
                  "Help do comando (man (default) | --help | --outra-opção) \n  [--help]: "
                  "Nome do utilitário (<Enter> para manter o nome ou :vazio para nome nulo) \n  [Name]: "
                  "\nO comando [cmd1] foi atualizado com sucesso.\n")
             (with-out-str
               (with-in-str "\n\nNew name\n" (actions/upd-command {:opts {:command "cmd1"}})))))
      (is (= {:command "cmd1" :description "Description 1 (more)" :doc "--help" :name "New name"}
             (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Update a command on database - Existing command - Keeping not null `name`"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite os novos valores, ou <Enter> para manter os valores atuais (mostrados entre colchetes).\n\n"
                  "Descrição do comando \n  [Description 1 (more)]: "
                  "Help do comando (man (default) | --help | --outra-opção) \n  [--help]: "
                  "Nome do utilitário (<Enter> para manter o nome ou :vazio para nome nulo) \n  [Name]: "
                  "\nO comando [cmd1] foi atualizado com sucesso.\n")
             (with-out-str
               (with-in-str "\n\n\n" (actions/upd-command {:opts {:command "cmd1"}})))))
      (is (= {:command "cmd1" :description "Description 1 (more)" :doc "--help" :name "Name"}
             (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Update a command on database - Existing command - Nulling originaly not null `name`"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite os novos valores, ou <Enter> para manter os valores atuais (mostrados entre colchetes).\n\n"
                  "Descrição do comando \n  [Description 1 (more)]: "
                  "Help do comando (man (default) | --help | --outra-opção) \n  [--help]: "
                  "Nome do utilitário (<Enter> para manter o nome ou :vazio para nome nulo) \n  [Name]: "
                  "\nO comando [cmd1] foi atualizado com sucesso.\n")
             (with-out-str
               (with-in-str "\n\n:vazio\n" (actions/upd-command {:opts {:command "cmd1"}})))))
      (is (= {:command "cmd1" :description "Description 1 (more)" :doc "--help" :name nil}
             (db/get-command-record "cmd1")))
      (fixtures/reset-database)))
  (testing "Update a command on database - Existing command - Keeping null `name`"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite os novos valores, ou <Enter> para manter os valores atuais (mostrados entre colchetes).\n\n"
                  "Descrição do comando \n  [Description 2]: "
                  "Help do comando (man (default) | --help | --outra-opção) \n  [--another]: "
                  "Nome do utilitário (<Enter> para manter o nome ou :vazio para nome nulo) \n  [:vazio]: "
                  "\nO comando [cmd2] foi atualizado com sucesso.\n")
             (with-out-str
               (with-in-str "\n\n\n" (actions/upd-command {:opts {:command "cmd2"}})))))
      (is (= {:command "cmd2" :description "Description 2" :doc "--another" :name nil}
             (db/get-command-record "cmd2")))
      (fixtures/reset-database)))
  (testing "Update a command on database - Existing command - Modifying originaly not null `name`"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= (str "Digite os novos valores, ou <Enter> para manter os valores atuais (mostrados entre colchetes).\n\n"
                  "Descrição do comando \n  [Description 2]: "
                  "Help do comando (man (default) | --help | --outra-opção) \n  [--another]: "
                  "Nome do utilitário (<Enter> para manter o nome ou :vazio para nome nulo) \n  [:vazio]: "
                  "\nO comando [cmd2] foi atualizado com sucesso.\n")
             (with-out-str
               (with-in-str "\n\nNew name\n" (actions/upd-command {:opts {:command "cmd2"}})))))
      (is (= {:command "cmd2" :description "Description 2" :doc "--another" :name "New name"}
             (db/get-command-record "cmd2")))
      (fixtures/reset-database)))
  (testing "Update a command on database - Inexisting command"
    (with-redefs [db/db  (str fixtures/temp-db-file)]
      (is (= "\nO comando [inexisting-cmd] não existe na base de dados.\n"
             (with-out-str
               (with-in-str "\n\nNew name\n" (actions/upd-command {:opts {:command "inexisting-cmd"}})))))
      (fixtures/reset-database))))
