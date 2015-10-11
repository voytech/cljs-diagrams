(ns impl.states.auth-states
  (:require [impl.api.public.auth :as a]
            [core.router.router :as r]
            [tailrecursion.javelin :refer [cell]])
  (:require-macros [tailrecursion.javelin :refer [defc= cell= dosync]]))

(def left-side-links (cell []))
(def right-side-links (cell []))

(defn- authenticated-navbar []
  (dosync
   (reset! right-side-links [;{:id "auth" :widget (login-widget (:identity a/login-state))}
                             {:id "logout" :title "Logout" :action #(r/goto-page "#/logout")}])
   (reset! left-side-links [])))

(defn- unauthenticated-navbar []
  (dosync
   (reset! right-side-links [{:id "login" :title "Login" :action #(r/goto-page "#/login")}
                             {:id "register" :title "Register" :action #(r/goto-page "#/register")}])
   (reset! left-side-links [])))

(defn- tenant-navbar []
  (dosync
   (reset! right-side-links [;{:id "auth" :widget (login-widget (:identity a/login-state))}
                             {:id "logout" :title "Logout" :action #(r/goto-page "#/logout")}])
   (reset! left-side-links [{:id "customize" :title "Customize" :action #(r/goto-page "#/login")}
                            {:id "customers" :title "Customers" :action #(r/goto-page "#/login")}
                            {:id "orders" :title "Orders" :action #(r/goto-page "#/login")}
                            {:id "messages" :title "Messages" :action #(r/goto-page "#/login")}])))

(cell= (cond
         (and (not (nil? (:identity a/login-state)))
              (= (-> a/login-state :role) :core.auth.roles/TENANT)
              (not (:initialized? a/login-state)))
         (do (println "goto register2step")
             (r/goto-page "#/register2step")
             (authenticated-navbar))

         (and (not (nil? (:identity a/login-state)))
              (= (-> a/login-state :role) :core.auth.roles/TENANT)
              (:initialized? a/login-state))
         (do (println "goto admin")
             (r/goto-page "#/admin")
             (tenant-navbar))

         (and (not (nil? (:identity a/login-state)))
              (= (-> a/login-state :role) :core.auth.roles/USER))
         (do (println "goto main")
             (r/goto-page "#/main")
             (authenticated-navbar))

         (nil? (:identity a/login-state))
         (do (println "goto register")
             (r/goto-page "#/register")
             (unauthenticated-navbar))))
