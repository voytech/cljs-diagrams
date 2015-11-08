(ns impl.api.public.auth
 (:require [tailrecursion.castra  :as c :refer [mkremote async jq-ajax]]
           [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
           [tailrecursion.javelin :as j :refer [cell]]
           [alandipert.storage-atom :refer [local-storage]])
 (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]]))

;;consider moving below state machine instrumentations into cljs/impl/states/
;;AUTH STATES:
(defc logout-state {})
(defc login-state {})
(defc error nil)
(defc loading [])

(defn reset-login-state []
  (swap! login-state dissoc :identity))

(def register  (mkremote 'core.services.public.auth/register
                          login-state
                          login-state
                          loading
                          ["/app/public"]))

(def logout    (mkremote 'core.services.public.auth/logout
                          logout-state
                          error
                          loading
                          ["/app/public"]))

(defn login [username password]
  (println (str "authenticating with " username " " password))
  (jq-ajax false
           "/app/login"
           nil
           {"authentication" (clj->cljson [username password])
            "Accept"          "application/json"}
           #(reset! login-state (cljson->clj %))
           #(reset! login-state (cljson->clj %))
           #()))

(defn is-login []
  (jq-ajax false
           "/app/is_login"
           nil
           {"Accept" "application/json"}
           #(reset! login-state (cljson->clj %))
           #(reset! login-state (cljson->clj %))
           #()))
