(ns impl.api.tenant.manage
  (:require [tailrecursion.castra  :as c :refer [mkremote async jq-ajax]]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [tailrecursion.javelin :as j :refer [cell]])
  (:require-macros
   [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc tenant-application-state {})
(defc submit-licence-state {})
(defc error {})
(defc loading [])

(def register2step  (mkremote 'core.services.tenant.manage/create-tenant
                              tenant-application-state
                              error
                              loading
                              ["/app/tenant"]))
