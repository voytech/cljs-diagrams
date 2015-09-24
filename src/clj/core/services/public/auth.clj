(ns core.services.public.auth
  (:require [tailrecursion.castra :refer [defrpc ex error *session*]]
            [cemerick.friend :refer [authenticated *identity*]]
            [impl.db.schema :refer :all]
            [core.db.schemap :refer :all]
            [datomic.api :as d]))

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

(defn prepare-entity [payload property]
  (-> payload clip (with-squuid property)))

(defn do-register [{:keys [username password re-password] :as payload}]
  (when-let [connection (d/connect *shared-db*)]
    (if (d/transact connection [(-> (prepare-entity payload :external-id) clj->db)])
      (when-let [dbval (d/db connection)]
        (if-let [fromdb (find-by-username dbval username)]
          (do
            (db->clj fromdb *shared-db*))
          (failure payload)))
      (failure payload))))

(defrpc register [{:keys [username password re-password] :as payload}]
  {:rpc/pre [(not (exists? username))
             (not= password re-password)]}
  (do-register payload))


(defrpc logout []
  (println "Logged out!"))
