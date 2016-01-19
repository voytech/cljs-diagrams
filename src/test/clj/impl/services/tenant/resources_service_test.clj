(ns impl.services.tenant.resources-service-test
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

(deftest test-create-user-resources
  (let [payload {:username "Adrian"
                 :password "UUUZDDD"
                 :re-password "UUUZDDD"
                 :role :core.auth.roles/TENANT
                 :identity "adrian"}
        details {:firstname "Adrian"
                 :lastname "Monk"
                 :email "adrian.monk@police.com"
                 :address-line-1 "San Francisco."}
        resource {:filename "text.html"
                  :data "<html><body>This will be just text</body></html>"
                  :content-type "text/html"
                  :category "background"}]


    (let [response (->>
                    (castra-request "/app/public" 'core.services.public.auth/register payload)
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.manage/create-tenant details)
                    (response-session)
                    (castra-request "/app/shared" 'core.services.shared.resources-service/put-resource resource))])))
