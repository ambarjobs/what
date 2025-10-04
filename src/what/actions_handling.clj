(ns what.actions-handling
  (:require
  ;;  [clojure.string :as string]
   [what.database :as db]))


(defn get-larger-cmd-size
  "Get size of the larger command."
  [command-records]
  (let [command-names (map #(:command %) command-records)]
    (apply max (map count command-names))))

(defn list-commands
  "List command and their descriptions."
  [_]
  (let [command-records (db/get-command-records)
        larger-cmd-size (get-larger-cmd-size command-records)
        cmd-format-str (str "%" larger-cmd-size "s:  %s")]

    (doseq [{:keys [command description name]} command-records]
      (let [formated-description (if (some? name) (format "[%s] %s" name description) description)]
        (println (format cmd-format-str command formated-description))))))
