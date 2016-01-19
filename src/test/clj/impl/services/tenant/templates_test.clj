(ns impl.services.tenant.templates-test
  (:require [core.services.tenant.templates-service :refer :all]
            [app :refer [app-handler]]
            [impl.services.test-helpers :refer :all]
            [clojure.test :refer :all]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [ring.mock.request :as mock]
            [impl.db.schema :refer :all]
            [core.db.schemap :refer [persist-schema]]
            [datomic.api :as d]
            [core.services.base :refer :all]))

(deftest test-create-template
  (let [payload {:username "Test001"
                 :password "UUUZDDD"
                 :re-password "UUUZDDD"
                 :role :core.auth.roles/TENANT
                 :identity "test001"}
        details {:firstname "Test 001"
                 :lastname "Test 001"
                 :email "adrian.monk@police.com"
                 :address-line-1 "San Francisco."}]

    (let [response (->>
                    (castra-request "/app/public" 'core.services.public.auth/register payload)
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.manage/create-tenant details)
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.templates-service/create-template nil))])))
