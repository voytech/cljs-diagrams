(ns ring.middleware.resource
   (:require [ring.util.codec :as codec]
            [ring.util.response :as response]
            [ring.util.request :as request]
            [cemerick.friend :as friend]
            [ring.middleware.head :as head]))

(defn- restrict-resources [request]
  (when (= :get (:request-method request))
    (let [path (subs (codec/url-decode (request/path-info request)) 1)
          username (:username (friend/current-authentication))]
      (when-not (empty? path)
        (println (str "serving : " path))
        ))
    )
)

(defn restrict-file-resources [handler]
  (fn [request]
    (or (restrict-resources request)
        (handler request))))
