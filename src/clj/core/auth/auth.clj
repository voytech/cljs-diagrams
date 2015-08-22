(ns core.auth.auth
  (:require [tailrecursion.castra.middleware :as c]
            [tailrecursion.castra  :as r]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [ring.util.request :as rur]
            [cemerick.friend.workflows :as cfw]
            [compojure.core    :refer :all]
            [cemerick.friend :as friend]
            [core.auth.roles :refer :all])
  )


(defn username-passwd-auth? [request]
  (let [[username password] (cljson->clj (rur/body-string request))]
    (if (and (not (nil? username))
             (not (nil? password)))
      {:username username
       :password password}
      false))
    )

(def global-credential-fn
  (fn [{:keys [username password]}]
      {:username username
       :roles [:core.auth.roles/USER]
       }))

(def global-unauthorized-handler
  (fn [req] {:status 403 :body (clj->cljson { :description "Unauthorized"
                                              :reason "Not suffucient priviledges"
                                              :identity (friend/current-authentication)})})
)

(def global-unauthenticated-handler
  (fn [req] {:status 401 :body (clj->cljson { :description "Unauthenticated"
                                              :reason "User is not authenticated"
                                              :identity (friend/current-authentication)})})
)

(def logged-in-handler
  (fn [req] {:status 200 :body (clj->cljson { :description "You are authenticated"
                                              :reason "Successful login"
                                              :identity (friend/current-authentication)})}))

;; ONLY tests.
(defn username-password-authentication-workflow
"This workflow provides standard username password authentication for application
Note that this workflow is out of the scope of castra RPC handler.
"
  [& {:keys [credential-fn realm] :as basic-config}]
  (fn [req]
    (println (:uri req))
    (when (re-find #"/login" (:uri req))
      (when-let [user-data (username-passwd-auth? req)]
        (-> (credential-fn {:username (:username user-data)
                            :password (:password user-data)})
            (cfw/make-auth {:cemerick.friend/workflow :castra
                            :cemerick.friend/redirect-on-auth? false}))))
    ))
