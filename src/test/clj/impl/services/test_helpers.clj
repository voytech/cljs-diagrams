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

(defn parse-resp [resp]
  {:body (-> resp
             :body
             cljson->clj)
   :status (:status resp)})
