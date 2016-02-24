(ns impl.api.public.auth
 (:require [tailrecursion.castra  :as c :refer [mkremote async jq-ajax]]
           [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
           [tailrecursion.javelin :as j :refer [cell]]
           [alandipert.storage-atom :refer [local-storage]]
           [core.api.base :as a])
 (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]]))

(def register  (mkremote 'core.services.public.auth/register
                          a/authentication
                          a/authentication
                          a/loading
                          ["/app/public"]))

(def logout    (mkremote 'core.services.public.auth/logout
                          a/authentication
                          a/error
                          a/loading
                          ["/app/public"]))

(defn login [username password]
  (jq-ajax false
           "/app/login"
           nil
           {"authentication" (clj->cljson [username password])
            "Accept"          "application/json"}
           #(reset! a/authentication (cljson->clj %))
           #(reset! a/authentication (cljson->clj %))
           #()))

(defn is-login []
  (jq-ajax false
           "/app/is_login"
           nil
           {"Accept" "application/json"}
           #(reset! a/authentication (cljson->clj %))
           #(reset! a/authentication (cljson->clj %))
           #()))
