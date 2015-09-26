(ns core.services.public.auth
  (:require [tailrecursion.castra :refer [defrpc ex error *session* *request*]]
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

; generalize this kind of call. Create  (persist (-> payload clip (with-squuid :external-id)) #(find-by-username % :username))
(defn do-register [{:keys [username password re-password] :as payload}]
  (when-let [connection (d/connect *shared-db*)]
    (if (d/transact connection [(-> payload clip (with-squuid :external-id) clj->db)])
      (when-let [dbval (d/db connection)]
        (if-let [fromdb (find-by-username dbval username)]
          (-> (db->clj fromdb *shared-db*)
              ffirst
              (dissoc :external-id))
          (failure payload)))
      (failure payload))))

(defrpc register [{:keys [username password re-password] :as payload}]
  ;; {:rpc/pre [(not (exists? username))
  ;;            (not= password re-password)]}
  (do-register payload))

(defrpc create-tenant [payload])

(defrpc logout []
  (println "Logged out!"))
