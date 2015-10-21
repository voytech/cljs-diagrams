(ns core.services.tenant.resources-service
  (:require [core.services.base :refer :all]
            [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [core.db.schemap :refer [persist-schema db->clj]]
            [impl.db.schema :refer :all]
            [cemerick.friend :as friend]
            [tailrecursion.extype :refer [defex extend-ex]]
            [datomic.api :as d]
            [clojure.java.io :as cjo]
            [conf :as cf]))

(defrpc make-category [data]
  (binding [*database-url* (tenant-db-url)]
    (store-entity data)))

(defrpc all-categories []
  (binding [*database-url* (tenant-db-url)]
    (query-by-property :resource.category/name)))


(defn- fs-path [username cat]
  (str username "/" cat))

(defn- fs-save [filename data]
  (let [abs-filename (str (:resource-path cf/configuration) filename)]
    (cjo/make-parents abs-filename)
    (spit abs-filename data)))

(defrpc put-resource [data]
  (let [ident (friend/current-authentication)
        username (:username ident)
        path (fs-path username (:category data))
        external-id (:external-id ident)]
    (binding [*database-url* (db-url (or (:identity ident) username))]
      (when-let [id (-> data
                        (assoc :owner external-id)
                        (assoc :path path)
                        (dissoc :data)
                        store-entity)]
        (fs-save (str path "/" (:filename data)) (:data data))
        {:resource-created true}))))

(defrpc all-resources []
  (binding [*database-url* (tenant-db-url)]
    (query-by-property :resource.file/filename)))

(defrpc get-resources [category]
  (binding [*database-url* (tenant-db-url)]
    (query-by-property :resource.file/category [:resource.category/name category])))

(defrpc get-resources-page [category {:keys [page-nr page-size] :as paging-opts}]
  )
