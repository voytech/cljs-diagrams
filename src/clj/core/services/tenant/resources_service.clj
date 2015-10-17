(ns core.services.tenant.resources-service
  (:require [core.services.base :refer :all]
            [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [core.db.schemap :refer [persist-schema]]
            [impl.db.schema :refer :all]
            [cemerick.friend :as friend]
            [tailrecursion.extype :refer [defex extend-ex]]
            [datomic.api :as d]))

(defrpc make-category [data]
  (store-entity data))

(defn- fs-path [username cat]
  (str username "/" cat))

(defn- fs-save [filename data]
  (make-parents filename)
  (spit filename data))

(defrpc put-resource [data]
  (let [ident (friend/current-authentication)
        username (:username ident)
        path (fs-path username (:category data))
        external-id (:external-id ident)]
    (binding [*database-url* (db-url (or (:identity ident) username))]
      (when-let [id (-> data
                        assoc :owner external-id
                        assoc :path path
                        store-entity)]
        (fs-save (str path "/" (:filename data)) (:data data))))))

(defrcp all-resources [])

(defrpc resources-by-type [type])
