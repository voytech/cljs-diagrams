(ns core.db.schemap-hooks
  (:require [datomic.api :as d]
            [core.db.schemap :refer [*db-url*]]))


(defn pull-property-hook [property]
  (fn [value]
    (-> (d/pull (d/db (d/connect *db-url*)) [property] (:db/id value))
        property)))

(defn extract-property-hook [property]
  (fn [value]
    (property value)))
