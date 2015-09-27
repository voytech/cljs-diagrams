(ns core.services.base
  (:require [core.db.schemap :refer :all]
            [datomic.api :as d]
            ))

(def ^:dynamic *database-url*)

(defn failed [input]
  (throw (ex-info "Could not process input!" input)))

(defn store-entity [entity]
  (when-let [connection (d/connect *database-url*)]
    (let [dbentity (-> entity clip clj->db)]
      (if-let [result (d/transact connection [dbentity])]
        (when-let [[tempids dbval] (->> result
                                        deref
                                        ((juxt :tempids :db-after)))]
            (d/resolve-tempid dbval tempids (:db/id dbentity)))
        (failed entity)))))

(defn load-entity [id]
  (when-let [connection (d/connect *database-url*)]
    (if-let [dbentity (d/entity (d/db connection) id)]
      (db->clj dbentity)
      (failed id))))
