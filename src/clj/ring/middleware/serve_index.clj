(ns ring.middleware.serve-index
   (:require [ring.util.codec :as codec]
            [ring.util.response :as response]
            [ring.util.request :as request]
            [ring.middleware.head :as head]))

(defn- index-file [request root-path]
  (when (= :get (:request-method request))
    (println (str (:request-method request) ":" (codec/url-decode (request/path-info request))))
    (let [path (subs (codec/url-decode (request/path-info request)) 1)]
      (when (empty? path)
        (println (str "serving index.html from " root-path "/index.html" ))
        (response/file-response "/index.html" {:root root-path})
            ))
    )
)

(defn wrap-index  [handler root-path]
  (fn [request]
    (or (index-file request root-path)
        (handler request))))
