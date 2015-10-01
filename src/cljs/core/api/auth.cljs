(ns core.api.auth
 (:require [tailrecursion.castra  :as c :refer [mkremote async]]
           [tailrecursion.javelin :as j :refer [cell]])
 (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc register-result {})
(defc logout-result {})
(defc login-result {})
(defc error nil)
(defc loading [])

(def register!  (mkremote 'core.services.public.auth/register
                          register-result
                          error
                          loading
                          ["/app/public"]))

(def logout!    (mkremote 'core.services.public.auth/logout
                          logout-result
                          error
                          loading
                          ["/app/public"]))

(defn login [username password]
  (async "/app/login" `[~username ~password]
         #(reset! login-result %) ;success handler
         #(reset! error %) ;error handler
         #() ;run always
         :ajax-impl nil))
