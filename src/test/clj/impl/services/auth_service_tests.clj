(ns impl.services.auth-service-tests
  (:require [core.services.public.auth :refer :all]
            [app :refer [app-handler]]
            [impl.services.test-helpers :refer :all]
            [clojure.test :refer :all]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [ring.mock.request :as mock]
            [impl.db.schema :refer :all]))

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
