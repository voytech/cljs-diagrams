(ns impl.api.tenant.templates
  (:require [tailrecursion.castra  :as c :refer [mkremote]]
            [tailrecursion.javelin :as j :refer [cell]]
            [core.project.settings :refer [settings!]]
            [core.project.project-services :refer [serialize-project-data
                                                   deserialize-project-data
                                                   cleanup-project-data]]
            [utils.dom.dom-utils :as dom])
  (:require-macros
   [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc state {})
(defc error nil)
(defc loading [])

(def save-template! (mkremote 'core.services.tenant.templates-service/save-template state error loading ["/core/services/tenant"]))
(def load-template! nil)
(def all-templates! nil)
(def templates-like! nil)
(def templates-paged! nil)
