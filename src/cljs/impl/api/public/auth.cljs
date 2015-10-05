(ns impl.api.public.auth
 (:require [tailrecursion.castra  :as c :refer [mkremote async jq-ajax]]
           [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
           [tailrecursion.javelin :as j :refer [cell]])
 (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc register-state {})
(defc logout-state {})
(defc login-state {})
(defc error nil)
(defc loading [])

(defn login-has-identity [state]
  (:identity state))

(defn register-check []
  )

(def register  (mkremote 'core.services.public.auth/register
                          register-state
                          error
                          loading
                          ["/app/public"]))

(def logout    (mkremote 'core.services.public.auth/logout
                          logout-state
                          error
                          loading
                          ["/app/public"]))

(defn login [username password]
  (jq-ajax false
           "/app/login"
           nil
           {"authentication" (clj->cljson [username password])
            "Accept"          "application/json"}
           #(reset! login-state (cljson->clj %))
           #(reset! error (cljson->clj %))
           #()))
