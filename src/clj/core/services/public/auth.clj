(ns core.services.public.auth
  (:require [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [cemerick.friend :refer [authenticated *identity*]]
            [cemerick.friend.workflows :as cfw]
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

;;TODO: Think if it is possible to make it via more public API of friend.
(defn friend-auth-support [auth]
  (let [f-auth  (-> (assoc auth :roles [(:role auth)])
                    (cfw/make-auth {:cemerick.friend/workflow :castra
                                    :cemerick.friend/redirect-on-auth? false}))]
    (swap! *session* assoc-in [:cemerick.friend/identity :authentications (:identity f-auth)] f-auth)
    (swap! *session* assoc-in [:cemerick.friend/identity :current] (:identity f-auth))))

(defrpc register [{:keys [username password re-password] :as payload}]
  {:rpc/pre [(not-exists username)
             (if-not (= password re-password)
               (throw (ex error (ex ::mismatch)))
               true)]
   :rpc/query [(user-query username :user.login/username *shared-db*)]}
  (binding [*database-url* *shared-db*]
    (when-let [id (-> payload
                      (with-squuid :external-id)
                      store-entity)]
      (-> (authenticate username password)
          (friend-auth-support)))))

(defrpc logout []
  (println "Logged out!"))
