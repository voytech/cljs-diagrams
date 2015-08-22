(ns core.api.api
 (:require [tailrecursion.castra  :as c :refer [mkremote]]
           [tailrecursion.javelin :as j :refer [cell]])
 (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc state {})
(defc error nil)
(defc loading [])

(def register!  (mkremote 'core.services.public.api/register  state error loading ["/core/services/public"]))
;(def login!     (mkremote 'core.services.public.api/login     state error loading ["/core/services/public"]))
(def logout!    (mkremote 'core.services.public.api/logout    state error loading ["/core/services/public"]))
