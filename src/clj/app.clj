(ns app
  (:require
   [ring.adapter.jetty               :refer [run-jetty]]
   [ring.middleware.resource         :refer [wrap-resource]]
   [ring.middleware.session          :refer [wrap-session]]
   [ring.middleware.session.cookie   :refer [cookie-store]]
   [ring.middleware.file             :refer [wrap-file]]
   [ring.middleware.file-info        :refer [wrap-file-info]]
   [tailrecursion.castra.handler     :refer [castra]]))

(def public-path "resources/public")

(defn run-app [port path]
  (->
    (castra 'core.api)
    (wrap-session {:store (cookie-store {:key "a 16-byte secret"})})
    (wrap-file (or path public-path))
    (wrap-file-info)
    (run-jetty {:join? true :port port})))
