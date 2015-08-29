(ns core.db.manager
  (:require [datomic.api :as d]
            ))

(def configuration {
                    :db-url "datomic:free://localhost:4334/"})

(def ^:const SHARED-DB "shared")

(defn load-configuration [config-file]
  (def configuration (load-string (slurp config-file)))
  configuration)

(defn initialize-database [dbname transaction-data]
  (let [connection-string (str (:db-url configuration) dbname)]
    (d/create-database connection-string)
    (when-let [connection (d/connect connection-string)]
      (d/transact connection transaction-data)))
  )
