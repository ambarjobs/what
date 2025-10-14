(ns what.actions-handling
  (:require
   [babashka.process :as proc]
   [what.database :as db]
   [what.utils :as utils]
   [what.config :refer [config-data]]))


(defn get-larger-cmd-size
  "Get size of the larger command."
  [command-records]
  (let [command-names (map #(:command %) command-records)]
    (apply max (map count command-names))))


(defn format-description
  "Format command's description to include its textual name if different from the command name."
  [description name]
  (if (some? name) (format "[%s] %s" name description) description))


(defn get-command-name
  "Get command name from ags or ask for one."
  [args]
  (let [{:keys [opts]} args
        {raw-command-name :command} opts]
    (or raw-command-name (utils/prompted-input "Digite o nome do comando: "))))


(defn list-commands
  "List command and their descriptions."
  [_]
  (let [command-records (db/get-command-records)
        larger-cmd-size (get-larger-cmd-size command-records)
        cmd-format-str (str "%" larger-cmd-size "s:  %s")]
    (doseq [{:keys [command description name]} command-records]
      (let [formated-description (format-description description name)]
        (println (format cmd-format-str command formated-description))))))


(defn show-command-info
  "Show information of a specific command."
  [args]
  (let [command-name (get-command-name args)
        result (db/get-command-record command-name)
        {:keys [command description name]} result]
    (if (some? command)
      (println (format "\n%s: %s" command (format-description description name)))
      (println (format "\nComando [%s] não encontrado na base de dados." (or command-name ""))))))


(defn add-command
  "Add a new command entry to the database."
  [args]
  (let [command-name  (get-command-name args)
        command-description (utils/prompted-input "Descrição do comando: ")
        raw-command-help (utils/prompted-input "Help do comando (man (default) | --help | --outra-opção): ")
        command-help (if (= raw-command-help "") "man" raw-command-help)
        raw-util-name (utils/prompted-input "Nome do utilitário (se diferente do nome do comando): ")
        util-name (if (= raw-util-name "") nil raw-util-name)]
    (try
      (let [result (db/insert-command-record command-name command-description command-help util-name)]
        (if (= (:rows-affected result) 1)
          (println (format "\nO comando [%s] foi adicionado com sucesso." command-name))
          (println (format "\nNão foi possível adicionar o comando [%s] à base de dados." command-name))))
      (catch Exception except
        (if (= (:cause (Throwable->map except)) "UNIQUE constraint failed: command.command")
          (println (format "\nO comando [%s] já existe na base de dados." command-name))
          (println (format "\nErro tentando adicionar o comando [%s] na base de dados." command-name)))))))


(defn rm-command
  "Remove a command from database."
  [args]
  (let [command-name  (get-command-name args)
        confirmation (utils/prompted-input (format "Deseja realmente REMOVER o comando [%s] (s/N): " command-name))]
    (when (some #{confirmation} ["s" "S"])
      (let [result (db/delete-command-record command-name)]
        (if (= (:rows-affected result) 1)
          (println (format "\nComando [%s] removido com sucesso." command-name))
          (println (format "\nO comando [%s] não existe na base de dados." (or command-name ""))))))))


(defn upd-command
  "Update the command fields."
  [args]
  (let [command-name  (get-command-name args)
        result (db/get-command-record command-name)]
    (if (some? result)
      (do
        (println "Digite os novos valores, ou <Enter> para manter os valores atuais (mostrados entre colchetes).\n")
        (let [{:keys [description doc name]} result
              raw-command-description (utils/prompted-input (format "Descrição do comando \n  [%s]: " description))
              command-description (if (= raw-command-description "") description raw-command-description)
              raw-command-help (utils/prompted-input (format "Help do comando (man (default) | --help | --outra-opção) \n  [%s]: " doc))
              command-help (if (= raw-command-help "") doc raw-command-help)
              raw-util-name (utils/prompted-input (format
                                                   "Nome do utilitário (ou :vazio para não dar um nome diferente do comando) \n  [%s]: " (or name ":vazio")))
              util-name (case raw-util-name
                          "" name
                          ":vazio" nil
                          raw-util-name)
              result (db/update-command-record command-name command-description command-help util-name)]
          (if (= (:rows-affected result) 1)
            (println (format "\nO comando [%s] foi atualizado com sucesso." command-name))
            (println (format "\nNão foi possível atualizar o comando [%s] à base de dados." command-name)))))
      (println (format "\nO comando [%s] não existe na base de dados." (or command-name ""))))))


(defn doc-command
  "Calls command's docs."
  [args]
  (let [command-name  (get-command-name args)
        result (db/get-command-record command-name)
        {:keys [command doc]} result]
    (if (some? command)
      (if (= doc "man")
        (proc/shell "man" command)
        (let [cmd-result (proc/shell {:out :string :continue true} command doc)]
          (proc/shell {:in (:out cmd-result) :continue true} (:pager-cmd config-data))))
      (println (format "Comando [%s] não encontrado na base de dados." (or command-name ""))))))


(defn get-command-urls
  "Get the URLs associated with a command."
  [args]
  (let [command-name (get-command-name args)
        result (db/get-command-record command-name)
        command-urls (map #(:url %) (db/get-command-urls command-name))]
    (if (some? result)
      (doseq [url command-urls]
        (println url))
      (println (format "Comando [%s] não encontrado na base de dados." (or command-name ""))))))


(defn debug
  "Action for debugging purposes."
  [_]
  (doseq [url (map #(:url %) (db/get-command-urls "dysk"))]
    ;; (utils/send-clipboard url)
    (println url)))
