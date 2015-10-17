(ns conf
  )

(def configuration {})


(defn load-configuration [config-file]
  (def configuration (load-string (slurp config-file)))
  configuration)
