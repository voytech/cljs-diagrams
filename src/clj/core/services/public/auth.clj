(ns core.services.public.auth
  (:require [tailrecursion.castra :refer [defrpc ex error *session*]]
            [cemerick.friend :refer [authenticated *identity*]]
            [impl.db.schema :refer :all]
            [datomic.api :refer d]))

(defn exists? [username]
  )

(defrpc register [{:keys [username password email] :as reg-payload}]
  (when-let [connection (d/connect *shared-db*)]
    (if (d/transact connection (clj->db (req-payload)))
      {:result :OK}
      {:result :FAIL})))


(defrpc logout []
  (println "Logged out!"))
