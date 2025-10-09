(ns what.config)


(def configuration-file "config.edn")


(defn read-config-file
  "Read a configuration file."
  [file-path]
  (read-string (slurp file-path)))


(defn load-config
  []
  (let [default-config {:pager-cmd "more"}
        config-content (read-config-file configuration-file)]
    (merge default-config config-content)))


(def config-data (load-config))
