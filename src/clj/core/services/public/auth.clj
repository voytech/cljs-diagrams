(ns core.services.public.auth
  (:require [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [cemerick.friend :refer [authenticated *identity*]]
            [impl.db.schema :refer :all]
            [core.db.schemap :refer [persist-schema]]
            [tailrecursion.extype :refer [defex extend-ex]]
            [core.services.base :refer :all]
            [datomic.api :as d]))

(extend-ex ::already-exists c/exception {:status 500} "User already exists")
(extend-ex ::mismatch c/exception {:status 500} "Passwords doesnt match")

(defn find-by-username [db value]
  (d/q '[:find (pull ?p [*])
         :in $ ?v
         :where [?p :user.login/username ?v]] db value))

(defn exists? [username]
  (when-let [exist (find-by-username (d/db (d/connect *shared-db*)) username)]
    (throw (ex "User already registered." username))))

(defn failure [input]
  (throw (ex "Could not process input!" input)))

(defn do-login [username password])

(defn with-squuid [payload property]
  (assoc payload property (d/squuid)))

(defn cp-property [source target property]
  (assoc target property (property source)))

(defrpc not-exists [username]
  (if-not (nil? (:username (load-entity [:user.login/username username] *shared-db*)))
    (throw (ex error (ex ::already-exists)))
    true))

(defn user-query [username qualified-prop db]
  (-> (load-entity [qualified-prop username] *shared-db*)
                   (dissoc :external-id :password :re-password)))

(defn authenticate [username password]
  (user-query username :user.login/username *shared-db*))

(defrpc register [{:keys [username password re-password] :as payload}]
  {:rpc/pre [(not-exists username)
             (if-not (= password re-password)
               (throw (ex error (ex ::mismatch)))
               true)]
   :rpc/query [(user-query username :user.login/username *shared-db*)]}
  (binding [*database-url* *shared-db*]
    (-> payload
        (with-squuid :external-id)
        store-entity)))

;username and its role is taken from *session* object
;tenant is assumed to be logged in. Also this method should be in
;tenat secured endpoint.
(defrpc create-tenant [{:keys [username] :as payload}]
  {:rpc/query [(user-query username :user.login/username *shared-db*)]}
  (let [user-data (load-entity [:user.login/username username] *shared-db*)
        dburl (db-url (or (:identity user-data)
                          (:username user-data)))
        uuid  (:external-id user-data)
        tenant-info (assoc payload :external-id uuid)]
    (println (str "tenant-info " tenant-info))
    (binding [*database-url* dburl]
      (when (d/create-database dburl)
        (persist-schema 'tenant dburl)
        (-> tenant-info
            store-entity)))))

(defrpc logout []
  (println "Logged out!"))
