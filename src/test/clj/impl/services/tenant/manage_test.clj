(ns impl.services.tenant.manage-test
  (:require [core.services.tenant.manage :refer :all]
            [app :refer [app-handler]]
            [impl.services.test-helpers :refer :all]
            [clojure.test :refer :all]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [ring.mock.request :as mock]
            [impl.db.schema :refer :all]
            [core.db.schemap :refer [persist-schema]]
            [datomic.api :as d]
            [core.services.base :refer :all]))

(deftest test-register-tenant
  (let [payload {:username "Test000"
                 :password "UUUZDDD"
                 :re-password "UUUZDDD"
                 :role :core.auth.roles/TENANT
                 :identity "test000"}
        details {:firstname "Test 000"
                 :lastname "Test 000"
                 :email "adrian.monk@police.com"
                 :address-line-1 "San Francisco."}]

    (let [response (->>
                    (castra-request "/app/public" 'core.services.public.auth/register payload)
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.manage/create-tenant details))])))
