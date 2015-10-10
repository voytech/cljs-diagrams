(ns core.services.tenant.manage
  (:require [core.services.base :refer :all]
            [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [core.db.schemap :refer [persist-schema]]
            [impl.db.schema :refer :all]
            [cemerick.friend :as friend]
            [tailrecursion.extype :refer [defex extend-ex]]
            [datomic.api :as d]))

(defn session-username []
  (:username (friend/current-authentication)))

(defrpc create-tenant [{:keys [firstname lastname email address-line-1 address-line-2 address-line-3] :as payload}]
  {:rpc/query [(user-query (session-username) :user.login/username *shared-db*)]}
  (let [user-data (load-entity [:user.login/username (session-username)] *shared-db*)
        dburl (db-url (or (:identity user-data)
                          (:username user-data)))
        uuid  (:external-id user-data)
        tenant-info (assoc payload :external-id uuid)]
    (binding [*database-url* dburl]
      (when (d/create-database dburl)
        (persist-schema 'tenant dburl)
        (-> tenant-info
            store-entity)))))
