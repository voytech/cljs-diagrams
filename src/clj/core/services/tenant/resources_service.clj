(ns core.services.tenant.resources-service
  (:require [core.services.base :refer :all]
            [tailrecursion.castra :as c :refer [defrpc ex error *session* *request* ]]
            [core.db.schemap :refer [persist-schema]]
            [impl.db.schema :refer :all]
            [cemerick.friend :as friend]
            [tailrecursion.extype :refer [defex extend-ex]]
            [datomic.api :as d]))

(defrpc make-category [data])

(defrpc put-resource [data])

(defrcp all-resources [])

(defrpc resources-by-type [type])
