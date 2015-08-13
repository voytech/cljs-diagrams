(ns app
  (:require
   [ring.adapter.jetty               :refer [run-jetty]]
   [ring.middleware.resource         :refer [wrap-resource]]
   [ring.middleware.session          :refer [wrap-session]]
   [ring.middleware.session.cookie   :refer [cookie-store]]
   [ring.middleware.file             :refer [wrap-file]]
   [ring.middleware.file-info        :refer [wrap-file-info]]
   [ring.middleware.serve-index      :refer [wrap-index]]
   [tailrecursion.castra.handler     :refer [castra]]
   ))

(def public-path "resources/public")

(def server (atom nil))
(def running (atom false))

(defn start [port path namespace join]
  (reset! server (->
                  (apply castra namespace)
                  (wrap-session {:store (cookie-store {:key "a 16-byte secret"})})
                  (wrap-file (or path public-path))
                  (wrap-index (or path public-path))
                  (wrap-file-info)
                  (run-jetty {:join? join :port port})))
  (reset! running true))

(defn run-app [port path]
  (start port path 'core.api true))
