(ns ring.middleware.resources
   (:require [ring.util.codec :as codec]
            [ring.util.response :as response]
            [ring.util.request :as request]
            [cemerick.friend :as friend]
            [ring.middleware.head :as head]
            [clojure.string :as str]))

(defn- restrict-resources [request unauthorized]
  (when (= :get (:request-method request))
    (let [path (subs (codec/url-decode (request/path-info request)) 1)
          ident (some identity ((juxt :identity :username) (friend/current-authentication)))]
      (when-not (empty? path)
        (let [path-ident (first (str/split path #"/"))]
          (when-not (or (= path-ident ident) (= path-ident "SHARED")) ;;generalize, pass which directory should have unauthorized access somewhere in params
            (unauthorized request)))))))

(defn restrict-file-resources [handler unauthorized]
  (fn [request]
    (or (restrict-resources request unauthorized)
        (handler request))))
