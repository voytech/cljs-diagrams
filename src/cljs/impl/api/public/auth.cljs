(ns impl.api.public.auth
 (:require [tailrecursion.castra  :as c :refer [mkremote async jq-ajax]]
           [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
           [tailrecursion.javelin :as j :refer [cell]])
 (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]]))

;;consider moving below state machine instrumentations into cljs/impl/states/
;;AUTH STATES:
(defc logout-state {})
(defc login-state {})
(defc error nil)
(defc loading [])

;;STATE QUERIES:
(defn logged-in?[]
  #(not (nil? (:identity login-state))))

(defn tenant-login? []
  (= (-> login-state :identity :role) :core.auth.roles/TENANT))

(defn tenant-initialized? []
  (when (tenant-login?)
    (-> login-state :identity :initialized?)))





(def register  (mkremote 'core.services.public.auth/register
                          login-state
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
