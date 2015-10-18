(ns impl.api.tenant.resource-api
  (:require [tailrecursion.castra  :as c :refer [mkremote async jq-ajax]]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [tailrecursion.javelin :as j :refer [cell]]
            [impl.api.public.auth :as a])
  (:require-macros
   [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc result {})
(defc loading [])

(def put-resource (mkremote 'core.services.tenant.resources-service/put-resource
                              a/result
                              a/result
                              loading
                              ["/app/tenant"]))
