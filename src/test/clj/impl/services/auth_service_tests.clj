(ns impl.services.auth-service-tests
  (:require [core.services.public.auth :refer :all]
            [app :refer [app-handler]]
            [clojure.test :refer :all]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [ring.mock.request :as mock]
            [impl.db.schema :refer :all]))

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

(deftest test-shared-schema
  (let [response (do-register {:username "Wojciech"
                               :password "123456789"
                               :role :core.auth.roles/TENANT
                               :identity "voy-tech"})]
    (println (str "reponse: " response))
    (is (= true true))))

(deftest test-register-rpc
  (let [payload {:username "Wojciech"
                 :password "987654321"
                 :role :core.auth.roles/TENANT
                 :identity "voytech"}]
    (is (= (parse-resp ((app-handler abspath) (mock-castra "/app/public"
                                                           'core.services.public.auth/register
                                                           {:username "Wojciech"
                                                            :password "987654321"
                                                            :role :core.auth.roles/TENANT
                                                            :identity "voytech"})))
           {:status  200
            :body payload}))))
