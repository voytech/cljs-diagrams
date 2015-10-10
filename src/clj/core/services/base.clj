(ns core.services.base
  (:require [core.db.schemap :refer :all]
            [impl.db.schema :refer :all]
            [datomic.api :as d]))

(def ^:dynamic *database-url*)

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
                      (dissoc :external-id :password :re-password))]
    (let [ident (or (:identity user) (:username user))
          dburl (db-url ident)]
      (if (some #{ident} (d/get-database-names (str (db-url) "*")))
        (assoc user :initialized? true)
        user))))
