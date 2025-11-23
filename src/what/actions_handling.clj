(ns what.actions-handling
  (:require
   [babashka.process :as proc]
   [clojure.string :as string]
   [what.config :refer [config-data]]
   [what.database :as db]
   [what.utils :as utils]))


(defn get-larger-cmd-size
  "Get size of the larger command."
  [command-records]
  (let [command-names (map #(:command %) command-records)]
    (if (empty? command-records) 0 (apply max (map count command-names)))))


(defn format-description
  "Format command's description to include its textual name if different from the command name."
  [description name]
  (if (some? name) (format "[%s] %s" name description) description))


(defn get-command-name
  "Get command name from args or ask for one."
  [args]
  (let [{:keys [opts]} args
        {raw-command-name :command} opts]
    (or raw-command-name (utils/prompted-input "Digite o nome do comando: "))))


(defn get-url
  "Get url from args or ask for one."
  [args]
  (let [{:keys [opts]} args
        {raw-command-name :url} opts]
    (or raw-command-name (utils/prompted-input "Digite o URL: "))))


(defn get-query-string
  "Get a query string from args or ask for one."
  [args]
  (let [{:keys [opts]} args
        {raw-query-string :query-string} opts]
    (or raw-query-string (utils/prompted-input "Digite a palavra a ser pesquisada: "))))


(defn show-command-record
  "Show information of a single command record."
  ([command-record] (show-command-record command-record true 1))
  ([command-record new-line?] (show-command-record command-record new-line? 1))
  ([command-record new-line? cmd-size] (let [{:keys [command description name]} command-record
                                             new-line (if new-line? "\n" "")
                                             cmd-format-str (str "%s%" cmd-size "s:  %s")
                                             formatted-description (format-description description name)]
                                         (when (some? command)
                                           (println (format cmd-format-str new-line command formatted-description))))))


;; ---------------------------------------------------------------------------------------------------------------------
;;   Actions
;; ---------------------------------------------------------------------------------------------------------------------

(defn list-commands
  "List command and their descriptions."
  [_]
  (let [command-records (db/get-command-records)
        larger-cmd-size (get-larger-cmd-size command-records)]
    (doseq [command-record command-records]
      (show-command-record command-record false larger-cmd-size))))


(defn show-command-info
  "Show information of a specific command."
  [args]
  (let [command-name (get-command-name args)
        result (db/get-command-record command-name)]
    (if (some? result)
      (show-command-record result)
      (println (format "\nComando [%s] não encontrado na base de dados." (or command-name ""))))))


(defn add-command
  "Add a new command entry to the database."
  [args]
  (let [command-name  (get-command-name args)]
    (if (empty? command-name)
      (println "\nO nome do comando deve ser fornecido.")
      (let [command-description (utils/prompted-input "Descrição do comando: ")
            raw-command-help (utils/prompted-input "Help do comando (man (default) | --help | :vazio para nulo): ")
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
              (println (format "\nErro tentando adicionar o comando [%s] na base de dados." command-name)))))))))


(defn rm-command
  "Remove a command from database."
  [args]
  (let [command-name  (get-command-name args)
        confirmation (utils/prompted-input (format "Deseja realmente REMOVER o comando [%s]? (s/N): " command-name))]
    (if (some #{confirmation} ["s" "S"])
      (let [result (db/delete-command-record command-name)]
        (if (= (:rows-affected result) 1)
          (doall
           (db/remove-command-urls-records command-name)
           (println (format "\nComando [%s] removido com sucesso." command-name)))
          (println (format "\nO comando [%s] não existe na base de dados." (or command-name "")))))
      (println (format "\nComando [%s] NÃO removido." command-name)))))


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
              raw-command-help (utils/prompted-input (format "Help do comando (man (default) | --help | :vazio para nulo) \n  [%s]: " (or doc ":vazio")))
              command-help (case raw-command-help
                             "" doc
                             ":vazio" nil
                             raw-command-help)
              raw-util-name (utils/prompted-input (format
                                                   "Nome do utilitário (<Enter> para manter o nome ou :vazio para nome nulo) \n  [%s]: " (or name ":vazio")))
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
      (case doc
        "man" (proc/shell "man" command)
        nil (println (format "\nNão existe indicação de documentação para o comando [%s] na base de dados." command))
        (let [cmd-result (proc/shell {:out :string :continue true} command doc)]
          (proc/shell {:in (:out cmd-result) :continue true} (:pager-cmd config-data))))
      (println (format "\nComando [%s] não encontrado na base de dados." (or command-name ""))))))


(defn get-command-urls
  "Get the URLs associated with a command."
  [args]
  (let [command-name (get-command-name args)
        result (db/get-command-record command-name)
        command-urls (map #(:url %) (db/get-command-urls-records command-name))]
    (if (some? result)
      (doseq [url command-urls]
        (println url))
      (println (format "\nComando [%s] não encontrado na base de dados." (or command-name ""))))))


(defn add-command-url
  "Add an URL association to a command."
  [args]
  (let [command-name (get-command-name args)
        url (get-url args)
        result (db/get-command-record command-name)
        command-urls (map #(:url %) (db/get-command-urls-records command-name))]
    (if (some? result)
      (if (some #{url} command-urls)
        (println (format "\nO comando [%s] já possui o URL [%s] associado a ele." command-name url))
        (db/add-command-url-record command-name url))
      (println (format "\nComando [%s] não encontrado na base de dados." (or command-name ""))))))


(defn remove-command-url
  "Remove an URL association to a command."
  [args]
  (let [command-name (get-command-name args)
        url (get-url args)
        result (db/get-command-record command-name)
        command-urls (map #(:url %) (db/get-command-urls-records command-name))]
    (if (some? result)
      (if (some #{url} command-urls)
          (db/remove-command-url-record command-name url)
          (println (format "\nO comando [%s] não possui o URL [%s] associado a ele." command-name url)))
      (println (format "\nComando [%s] não encontrado na base de dados." (or command-name ""))))))


(defn show-fields
  "Show the fields used to filter the query for a command or description."
  [fields]
  (string/join " " (map name fields)))


(defn find-command
  "Find a command containing a query-string on the fields selected."
  [args]
  (let [query-string (get-query-string args)]
    (if (empty? query-string)
      (println "\nUma palavra de pesquisa deve ser fornecida.")
      (let [raw-fields-string (utils/prompted-input "\nDigite os campos a serem pesquisados (<Enter>: command description): ")
            fields-string (if (= raw-fields-string "") "command description" raw-fields-string)
            fields (utils/process-string-list fields-string keyword)
            query-fields (if (some #{:description} fields) (conj fields :name) fields)
            command-records (db/find-command-records query-string query-fields)
            larger-cmd-size (get-larger-cmd-size command-records)]
        (if (empty? command-records)
          (println
           (format "\nNão foram encontrados comandos com a palavra [%s] nos campos [%s]." query-string (show-fields fields)))
          (do
            (println)
            (doseq [command-record command-records]
              (show-command-record command-record false larger-cmd-size))))))))


(defn debug
  "Action for debugging purposes."
  [_]
  ())
