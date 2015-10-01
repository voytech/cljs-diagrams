(ns core.services.tenant.manage
  (:require [core.services.base :refer :all]
            [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [core.db.schemap :refer [persist-schema]]
            [impl.db.schema :refer :all]
            [tailrecursion.extype :refer [defex extend-ex]]
            [datomic.api :as d]))

(defrpc create-tenant [{:keys [username] :as payload}]
  {:rpc/query [(user-query username :user.login/username *shared-db*)]}
  (let [user-data (load-entity [:user.login/username username] *shared-db*)
        dburl (db-url (or (:identity user-data)
                          (:username user-data)))
        uuid  (:external-id user-data)
        tenant-info (assoc payload :external-id uuid)]
    (binding [*database-url* dburl]
      (when (d/create-database dburl)
        (persist-schema 'tenant dburl)
        (-> tenant-info
            store-entity)))))
