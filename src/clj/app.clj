(ns app
  (:require
   [ring.adapter.jetty               :refer [run-jetty]]
   [ring.middleware.resource         :refer [wrap-resource]]
   [ring.middleware.session          :refer [wrap-session]]
   [ring.middleware.session.cookie   :refer [cookie-store]]
   [ring.middleware.file             :refer [wrap-file]]
   [ring.middleware.file-info        :refer [wrap-file-info]]
   [ring.middleware.serve-index      :refer [wrap-index]]
   [ring.middleware.params           :refer [wrap-params]]
   [ring.middleware.nested-params    :refer [wrap-nested-params]]
   [ring.middleware.keyword-params   :refer [wrap-keyword-params]]
  ; [tailrecursion.castra.handler     :refer [castra]]
   [core.auth.castra-endpoints       :refer [restricted-castra-routes]]
   [core.auth.auth                   :refer [global-credential-fn
                                             global-unauthorized-handler
                                             global-unauthenticated-handler
                                             username-password-authentication-workflow]]
   [cemerick.friend                  :as friend]
   [compojure.core :refer :all]
   [compojure.route :as route]
   ))

(def public-path "resources/public")
(def server (atom nil))
(def running (atom false))

(defn start [port path namespace join]
  (reset! server (->
                  (apply routes
                         (restricted-castra-routes [{:namespace 'core.services.tenant
                                                     :roles [:TENANT]}
                                                    {:namespace 'core.services.user
                                                     :roles [:TENANT :USER]}
                                                    {:namespace 'core.services.public ;Empty vector indicates not authorized access.
                                                     :roles []}]))

                  (friend/authenticate {:unauthorized-handler    global-unauthorized-handler
                                        :unauthenticated-handler global-unauthenticated-handler
                                        :allow-anon? false
                                        :workflows [(username-password-authentication-workflow
                                                    :credential-fn global-credential-fn)
                                                    ]})
                  (wrap-session {:store (cookie-store {:key "a 16-byte secret"})})
                  (wrap-file  (or path public-path))
                  (wrap-index (or path public-path))
                  (wrap-file-info)
                  (wrap-keyword-params)
                  (wrap-nested-params)
                  (wrap-params)
                  (run-jetty {:join? join :port port})))
  (reset! running true))

(defn run-app [port path]
  (start port path 'core.api true))
