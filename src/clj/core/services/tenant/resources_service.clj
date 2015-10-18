(ns core.services.tenant.resources-service
  (:require [core.services.base :refer :all]
            [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [core.db.schemap :refer [persist-schema]]
            [impl.db.schema :refer :all]
            [cemerick.friend :as friend]
            [tailrecursion.extype :refer [defex extend-ex]]
            [datomic.api :as d]
            [conf :as cf]))

(defrpc make-category [data]
  (let [ident (friend/current-authentication)]
    (binding [*database-url* (db-url (or (:identity ident) (:username ident)))]
      (store-entity data))))

(defn- fs-path [username cat]
  (str username "/" cat))

(defn- fs-save [filename data]
  (let [abs-filename (str (:resource-path cf/configuration)
                          filename)])
  (make-parents abs-filename)
  (spit abs-filename data))

(defrpc put-resource [data]
  (let [ident (friend/current-authentication)
        username (:username ident)
        path (fs-path username (:category data))
        external-id (:external-id ident)]
    (binding [*database-url* (db-url (or (:identity ident) username))]
      (when-let [id (-> data
                        assoc :owner external-id
                        assoc :path path
                        dissoc :data
                        store-entity)]
        (fs-save (str path "/" (:filename data)) (:data data))))))

(defrcp all-resources [])

(defrpc resources-by-type [type])
