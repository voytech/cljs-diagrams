(ns core.services.tenant.manage
  (:require [core.services.base :refer :all]
            [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [core.db.schemap :refer [persist-schema]]
            [impl.db.schema :refer :all]
            [cemerick.friend :as friend]
            [core.services.tenant.templates-service :as ts]
            [core.services.shared.resources-service :as rs]
            [tailrecursion.extype :refer [defex extend-ex]]
            [datomic.api :as d]))

(defn session-username []
  (:username (friend/current-authentication)))

(defn create-resource-categories []
  (let [url (tenant-db-url)]
    (rs/make-category url {:name "background" :description "Canvas backgrounds"})
    (rs/make-category url {:name "clipart" :description "Clipart is a small picture widget to be placed somewhere"})
    (rs/make-category url {:name "photo" :description "Photo."})))

(defn initialize-tenant-space []
  (create-resource-categories)
  (ts/create-template))

(defrpc create-tenant [{:keys [firstname lastname email address-line-1 address-line-2 address-line-3] :as payload}]
  {:rpc/query [(friend-refresh-session (session-username) :user.login/username *shared-db*)]}
  (println "create tenant")
  (let [user-data (load-entity [:user.login/username (session-username)] *shared-db*)
        dburl (db-url (or (:identity user-data)
                          (:username user-data)))
        uuid  (:external-id user-data)
        tenant-info (assoc payload :external-id uuid)]
    (println (str "Creating database: " dburl))
    (binding [*database-url* dburl]
      (when (d/create-database dburl)
        (println "initializing tenant schema...")
        (persist-schema 'tenant dburl)
        (println "tenant schema initialized...")
        (initialize-tenant-space)
        (-> tenant-info
            store-entity)))))
