(ns impl.services.public.auth-tests
  (:require [core.services.public.auth :refer :all]
            [app :refer [app-handler]]
            [impl.services.test-helpers :refer :all]
            [clojure.test :refer :all]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [ring.mock.request :as mock]
            [impl.db.schema :refer :all]
            [datomic.api :as d]
            [core.services.base :refer :all]))

(deftest test-register-rpc
  (let [payload {:username "Wojciech"
                 :password "UUUZDDD"
                 :role :core.auth.roles/TENANT
                 :identity "voytech"}]
    (is (= (parse-resp ((app-handler abspath) (mock-castra "/app/public"
                                                           'core.services.public.auth/register
                                                           payload)))
           {:status  200
            :body (dissoc payload :password)}))))

(deftest test-create-tenant-rpc)
