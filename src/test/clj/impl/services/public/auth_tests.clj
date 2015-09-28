(ns impl.services.public.auth-tests
  (:require [core.services.public.auth :refer :all]
            [app :refer [app-handler]]
            [impl.services.test-helpers :refer :all]
            [clojure.test :refer :all]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [ring.mock.request :as mock]
            [impl.db.schema :refer :all]
            [core.db.schemap :refer [persist-schema]]
            [datomic.api :as d]
            [core.services.base :refer :all]))

(defn- reload-db [name url]
  (d/delete-database url)
  (when (d/create-database url)
    (d/connect url)
    (persist-schema name url)))

(deftest test-register-rpc
  ;(reload-db 'shared *shared-db*)
  (let [payload {:username "Wojciech"
                 :password "UUUZDDD"
                 :re-password "UUUZDDD"
                 :role :core.auth.roles/TENANT
                 :identity "voytech"}]
    (is (= (parse-resp ((app-handler abspath) (mock-castra "/app/public"
                                                           'core.services.public.auth/register
                                                           payload)))
           {:status  200
            :body (dissoc payload :password :re-password)}))))

(deftest test-create-tenant-rpc)
