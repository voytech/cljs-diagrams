(ns impl.services.test-helpers
  (:require [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [ring.mock.request :as mock]))

(def abspath "/home/voytech/programming/github/photo-collage/resources")

(defn mock-castra [path q-func payload]
  (-> (mock/request :post path)
      (mock/header "X-Castra-Csrf" "true")
      (mock/header "X-Castra-Tunnel" "cljson")
      (mock/header "Accept" "application/json")
      (mock/body (clj->cljson [q-func payload]))))

(defn mock-login [path username password]
  (-> (mock/request :post path)
      (mock/header "Authentication" (clj->cljson [username password]))
      (mock/header "Accept" "application/json")))

(defn parse-resp [resp]
  {:body (-> resp
             :body
             cljson->clj)
   :status (:status resp)})

(defn response-session [response]
  (-> response
      :headers
      (get "Set-Cookie")
      (first)))

(defn ensure-session [request session-value]
  (-> request
      assoc-in [:headers "cookie"] session-value))

(defmacro with-session [username password & body]
  `(let [response# (impl.services.test-helpers/mock-login "/app/login" ~username ~password)
         session# (impl.services.test-helpers/response-session response#)]
     ))
