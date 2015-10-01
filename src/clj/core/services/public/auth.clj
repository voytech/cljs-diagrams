(ns core.services.public.auth
  (:require [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [cemerick.friend :refer [authenticated *identity*]]
            [impl.db.schema :refer :all]
            [core.db.schemap :refer [persist-schema]]
            [tailrecursion.extype :refer [defex extend-ex]]
            [core.services.base :refer :all]
            [datomic.api :as d]))

(extend-ex ::already-exists c/exception {:status 500} "User already exists")
(extend-ex ::mismatch c/exception {:status 500} "Passwords does not match")

(defn with-squuid [payload property]
  (assoc payload property (d/squuid)))

(defrpc not-exists [username]
  (if-not (nil? (:username (load-entity [:user.login/username username] *shared-db*)))
    (throw (ex error (ex ::already-exists)))
    true))

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

(defrpc logout []
  (println "Logged out!"))
