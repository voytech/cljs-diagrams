(ns impl.api.tenant.manage
  (:require [tailrecursion.castra  :as c :refer [mkremote async jq-ajax]]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [tailrecursion.javelin :as j :refer [cell]]
            [core.api.base :as a])
  (:require-macros
   [tailrecursion.javelin :refer [defc defc= cell=]]))

;; (defc error {})
;; (defc loading [])

(def register2step  (mkremote 'core.services.tenant.manage/create-tenant
                              a/authentication
                              a/authentication
                              a/loading
                              ["/app/tenant"]))
