(ns core.api.auth
 (:require [tailrecursion.castra  :as c :refer [mkremote async]]
           [tailrecursion.javelin :as j :refer [cell]])
 (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc state {})
(defc error nil)
(defc loading [])

(def register!  (mkremote 'core.services.public.auth/register  state error loading ["/core/services/public"]))

(def logout!    (mkremote 'core.services.public.auth/logout    state error loading ["/core/services/public"]))

(defn login [username password]
  (async "/login" `[~username ~password]
         #() ;success handler
         #() ;error handler
         #() ;run always
         :ajax-impl nil))
