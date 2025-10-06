(ns what.actions-handling
  (:require
  ;;  [clojure.string :as string]
   [what.database :as db]))


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
