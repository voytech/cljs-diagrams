(ns core.db.manager
  (:require [datomic.api :as d]
            ))

(def configuration {
                    :db-url "datomic:free://localhost:4334/"})

(def ^:const SHARED-DB "shared")

(defn load-configuration [config-file]
  (def configuration (load-string (slurp config-file)))
  configuration)

(defn initialize-database
"Initializes database with name given as first param.
It creates db when it is not created and performs initialization
with transaction data passed as second argument. Important note
is that connection is cached as provided by datomic api thus we
will use it further no matter if call connect once again - we
do not need to keep connection var. "
[dbname transaction-data]
  (let [connection-string (str (:db-url configuration) dbname)]
    (d/create-database connection-string)
    (when-let [connection (d/connect connection-string)]
      (d/transact connection transaction-data))))
