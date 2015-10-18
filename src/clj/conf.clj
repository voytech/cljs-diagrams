(ns conf)

(def CONF_FILE "resources/schema/properties.edn")
(def configuration {})


(defn load-configuration [config-file]
  (def configuration (load-string (slurp config-file)))
  configuration)
