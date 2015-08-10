(ns core.api.api
 (:require [tailrecursion.castra  :as c :refer [mkremote]]
           [tailrecursion.javelin :as j :refer [cell]])
 (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc state {})
(defc error nil)
(defc loading [])

(def register!  (mkremote 'core.api/register  state error loading))
(def login!     (mkremote 'core.api/login     state error loading))
(def logout!    (mkremote 'core.api/logout    state error loading))
