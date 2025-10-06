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
    (println args)
    (cond (not (some? command-name)) (println "É necessário informar o nome do comando procurado. Ex: what is <comando>.")
          (not (some? command)) (println (format "Comando [%s] não encontrado." (or command-name "")))
          :else (println (format "%s: %s" command (format-description description name))))))


(defn add-command
  "Adiciona um novo comando na base de dados."
  [args]
  (let [{:keys [opts]} args
        {command-name :command} opts
        command-description (utils/prompted-input "Descrição do comando: ")
        raw-command-help (utils/prompted-input "Help do comando (man (default) | --help | --outra-opção): ")
        command-help (if (= raw-command-help "") "man" raw-command-help)
        raw-util-name (utils/prompted-input "Nome do utilitário (se diferente do nome do comando): ")
        util-name (if (= raw-util-name "") nil raw-util-name)]
    (try
      (db/add-command command-name command-description command-help util-name)
      (catch Exception except
        (if (= (:cause (Throwable->map except)) "UNIQUE constraint failed: command.command")
          (println (format "\nO comando [%s] já existe na base de dados." command-name))
          (println (format "\nErro tentando adicionar o comando [%s] na base de dados." command-name)))))))
