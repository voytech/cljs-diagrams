(ns core.services.base
  (:require [core.db.schemap :refer :all]
            [impl.db.schema :refer :all]
            [tailrecursion.castra :as c :refer [*session* *request* ]]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as cfw]
            [datomic.api :as d]))

(def ^:dynamic *database-url*)
(declare user-query)

(defmacro limit
  ([coll start-from quantity]
   `(take ~quantity (drop ~start-from ~coll)))
  ([coll quantity]
   `(limit ~coll 0 ~quantity)))

(defn query-by-property
  ([property value]
   (let [matches (if value (if (string? value) (str "\"" value "\"") (str value)) "?val")
         query-str (str "[:find (pull ?e [*]) :in $ :where [?e " (str property) (if value (str )) " " matches " ]]")
         result (flatten (d/q query-str (d/db (d/connect *database-url*))))]
     (db->clj result *database-url*)))
  ([property] (query-by-property property nil)))

(defn tenant-db-url []
  (let [current-auth (friend/current-authentication)
        is-tenant (= (:role current-auth) :core.auth.roles/TENANT)
        tenant (:tenant current-auth)
        ident (or (:identity current-auth) (:username current-auth))]
    (if is-tenant
      (db-url ident)
      (when-let [tenant (user-query tenant :user.login/username *shared-db*)]
        (db-url (or (:identity tenant) (:username tenant)))))))

(defn failed [input]
  (throw (ex-info "Could not process input!" input)))

(defn store-entity
  ([entity dburl]
   (when-let [connection (d/connect dburl)]
     (let [dbentity (-> entity clip clj->db)]
       (if-let [result (d/transact connection [dbentity])]
         (when-let [[tempids dbval] (->> result
                                         deref
                                         ((juxt :tempids :db-after)))]
           (d/resolve-tempid dbval tempids (:db/id dbentity)))
         (failed entity)))))
  ([entity] (store-entity entity *database-url*)))

(defn load-entity
  ([id dburl]
   (when-let [connection (d/connect dburl)]
     (if-let [dbentity (d/pull (d/db connection) '[*] id)]
       (db->clj dbentity dburl)
       (failed id))))
  ([id] (load-entity id *database-url*)))

(defn user-query [username qualified-prop db]
  (when-let [user (-> (load-entity [qualified-prop username] *shared-db*)
                      (dissoc :password :re-password))]
    (let [ident (or (:identity user) (:username user))
          dburl (db-url ident)]
      (if (= (:role user) :core.auth.roles/TENANT)
        (if (some #{ident} (d/get-database-names (str (db-url) "*")))
          (assoc user :initialized? true)
          user)))))

(defn friend-refresh-session
  ([auth]
   (let [f-auth  (-> (assoc auth :roles [(:role auth)])
                     (cfw/make-auth {:cemerick.friend/workflow :castra
                                     :cemerick.friend/redirect-on-auth? false}))]
     (swap! *session* assoc-in [:cemerick.friend/identity :authentications (:identity f-auth)] f-auth)
     (swap! *session* assoc-in [:cemerick.friend/identity :current] (:identity f-auth))))
  ([username qualified-prop db]
   (-> (user-query username qualified-prop db)
       (friend-refresh-session))))
