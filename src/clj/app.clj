(ns app
  (:require
   [ring.adapter.jetty               :refer [run-jetty]]
   [ring.middleware.resource         :refer [wrap-resource]]
   [ring.middleware.resources        :refer [restrict-file-resources]]
   [ring.middleware.session          :refer [wrap-session]]
   [ring.middleware.session-timeout  :refer [wrap-idle-session-timeout]]
   [ring.middleware.session.cookie   :refer [cookie-store]]
   [ring.middleware.file             :refer [wrap-file]]
   [ring.middleware.file-info        :refer [wrap-file-info]]
   [ring.middleware.serve-index      :refer [wrap-index]]
   [ring.middleware.params           :refer [wrap-params]]
   [ring.middleware.nested-params    :refer [wrap-nested-params]]
   [ring.middleware.keyword-params   :refer [wrap-keyword-params]]
   [ring.middleware.stacktrace       :refer [wrap-stacktrace]]
   [core.auth.castra-endpoints       :refer [restricted-castra-routes]]
   [core.auth.auth                   :refer [global-credential-fn
                                             global-unauthorized-handler
                                             global-unauthenticated-handler
                                             logged-in-handler
                                             is-logged-handler
                                             timeout-response
                                             username-password-authentication-workflow]]
   [cemerick.friend                  :as friend]
   [compojure.core :refer :all]
   [compojure.route :as route]
   [core.auth.roles :refer :all]
   [conf :refer :all]
   ))

(def public-path "resources/public")
(def resource-path (:resource-path (load-configuration "resources/schema/properties.edn")))
(def server (atom nil))
(def running (atom false))

(load-configuration CONF_FILE)

(def app-handler (fn [path] (-> (apply routes
                                       (concat
                                        [(POST "/app/is_login" [] is-logged-handler)]
                                        [(POST "/app/login" [] logged-in-handler)]
                                        (restricted-castra-routes [{:namespace 'core.services.tenant
                                                                    :roles [:core.auth.roles/TENANT]
                                                                    :path "/app/tenant"}
                                                                   {:namespace 'core.services.user
                                                                    :roles [:core.auth.roles/USER]
                                                                    :path "/app/user"}
                                                                   {:namespace 'core.services.shared
                                                                    :roles [:core.auth.roles/TENANT :core.auth.roles/USER]
                                                                    :path "/app/shared"}
                                                                   {:namespace 'core.services.public ;Empty vector indicates not authorized access.
                                                                    :roles []
                                                                    :path "/app/public"}])))
                                (wrap-file resource-path)
                                (restrict-file-resources global-unauthorized-handler)
                                (friend/authenticate {:unauthorized-handler    global-unauthorized-handler
                                                      :unauthenticated-handler global-unauthenticated-handler
                                                      :allow-anon? true
                                                      :workflows [(username-password-authentication-workflow
                                                                   :credential-fn global-credential-fn)]})
                                (wrap-idle-session-timeout {:timeout 300 :timeout-response timeout-response})
                                (wrap-session {:store (cookie-store {:key "a 16-byte secret"})})
                                (wrap-file  (or path public-path))
                                (wrap-index (or path public-path))
                                (wrap-file-info)
                                (wrap-keyword-params)
                                (wrap-nested-params)
                                (wrap-params)
                                (wrap-stacktrace))))

(defn start [port path join]
  (reset! server (run-jetty (app-handler path) {:join? join :port port}))
  (reset! running true))
