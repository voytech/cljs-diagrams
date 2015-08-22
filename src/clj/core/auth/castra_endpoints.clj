(ns core.auth.castra-endpoints
  (:require [cemerick.friend   :as friend]
            [compojure.core    :refer :all]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [tailrecursion.castra.middleware :as c]
            [tailrecursion.castra  :as r]
            [tailrecursion.castra.handler :refer [castra]]
            [ring.util.request :as rur]
            [cemerick.friend.workflows :as cfw]
            [cognitect.transit   :as t]
            [bultitude.core :as b])
  (:import  [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn protect-castra-route
"Defines ring handlers chain consisted from:
 - compojure/POST,
 - friend/wrap-authorize
 - castra endpoint handler
Goal is to create a request mapping for path determined by namespace,
wrap this route by friend/wrap-authorize giving roles passed as params vector and
finally pass castra handler restricted to all child namespaces of main namespace"

[{:keys [namespace roles]}]
  (when-not (vector? roles) (throw (IllegalArgumentException. "roles parameter must be vector")))
  (when (symbol? namespace)
    (let [namespace-str (name namespace)
          req-path (clojure.string/replace namespace-str #"\." "/")
          child-ns (b/namespaces-on-classpath :prefix namespace-str)]
      (println (str "Root namespace <" namespace-str "> is served on request path <" req-path ">"))
      (println (str "RPC requests are allowed on following namespaces: "))
      (println child-ns)
      (POST (if-not (.startsWith req-path "/") (str "/" req-path) req-path)
            []
            (if (empty? roles)
                 (apply castra child-ns)
                 (friend/wrap-authorize (apply castra child-ns) roles))
            ))))

(defn restricted-castra-routes [castra-routes]
  (doall (map #(protect-castra-route %) castra-routes)))
