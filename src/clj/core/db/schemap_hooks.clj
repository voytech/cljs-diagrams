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

(defn extract-or-pull-property-hook [property]
  (fn [value]
    (or ((extract-property-hook property) value)
        ((pull-property-hook) value))))
