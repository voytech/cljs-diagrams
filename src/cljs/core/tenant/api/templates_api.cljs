(ns core.tenant.api.templates-api
  (:require [tailrecursion.castra  :as c :refer [mkremote]]
            [tailrecursion.javelin :as j :refer [cell]])
  (:require-macros
   [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc state {})
(defc error nil)
(defc loading [])

(def save-template! (mkremote 'core.services.tenant.templates-service/save-template state error loading :url "/core/services/tenant"))
(def load-template! nil)
(def all-templates! nil)
(def templates-like! nil)
(def templates-paged! nil)
