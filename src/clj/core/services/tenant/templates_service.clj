(ns core.services.tenant.templates-service
  (:require [tailrecursion.castra :refer [defrpc ex error *session*]]))

(defrpc save-template [template-data]
  (println template-data))
