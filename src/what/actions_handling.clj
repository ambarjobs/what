(ns what.actions-handling
  (:require
   ;;  [clojure.string :as string]
   [what.database :as db]
   [what.utils :as utils]))


(defn get-larger-cmd-size
  "Get size of the larger command."
  [command-records]
  (let [command-names (map #(:command %) command-records)]
    (apply max (map count command-names))))


(defn format-description
  "Format command's description to include its textual name if different from the command name."
  [description name]
  (if (some? name) (format "[%s] %s" name description) description))


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
  "Show the command information."
  [args]
  (let [{:keys [opts]} args
        {command-name :command} opts
        {:keys [command description name]} (db/get-command-record command-name)]
    (cond (not (some? command-name)) (println "É necessário informar o nome do comando procurado. Ex: what is <comando>.")
          (not (some? command)) (println (format "Comando [%s] não encontrado." (or command-name "")))
          :else (println (format "%s: %s" command (format-description description name))))))


(defn add-command
  "Add a new command entry to the database."
  [args]
  (let [{:keys [opts]} args
        {raw-command-name :command} opts
        command-name (or raw-command-name (utils/prompted-input "Digite o nome do comando: "))
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
  (let [{:keys [opts]} args
        {raw-command-name :command} opts
        command-name (or raw-command-name (utils/prompted-input "Digite o nome do comando: "))
        confirmation (utils/prompted-input (format "Deseja realmente REMOVER o comando [%s] (s/N): " command-name))]
    (when (some #{confirmation} ["s" "S"])
      (let [result (db/delete-command-record command-name)]
        (if (= (:rows-affected result) 1)
          (println (format "\nComando [%s] removido com sucesso." command-name))
          (println (format "\nO comando [%s] não existe na base de dados." command-name)))))))


(defn upd-command
  "Update the command fields."
  [args]
  (let [{:keys [opts]} args
        {raw-command-name :command} opts
        command-name (or raw-command-name (utils/prompted-input "Digite o nome do comando: "))
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
      (println (format "\nO comando [%s] não existe na base de dados." command-name)))))
