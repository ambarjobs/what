(ns what.config
  (:require [clojure.java.io :as io]))


(def configuration-file (io/resource "config.edn"))

(defn read-config-file
  "Read a configuration file."
  [config-file]
  (read-string (slurp config-file)))


(defn load-config
  "Load configuration from configuration file."
  []
  (let [default-config {:pager-cmd "more"}
        config-content (read-config-file configuration-file)]
    (merge default-config config-content)))


(def config-data (load-config))
