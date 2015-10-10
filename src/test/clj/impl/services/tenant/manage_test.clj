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

(deftest test-register-rpc
  (reload-db 'shared *shared-db*)
  (let [payload {:username "Adrien"
                 :password "UUUZDDD"
                 :re-password "UUUZDDD"
                 :role :core.auth.roles/TENANT
                 :identity "adrien"}
        details {:firstname "Adrien"
                 :lastname "Monk"
                 :email "adrien.monk@police.com"
                 :address-line-1 "San Francisco."}]
    (let [response ((app-handler abspath) (mock-castra "/app/public"
                                                       'core.services.public.auth/register
                                                       payload))
          body (parse-resp response)]
      (println (str "session id 1:" (response-session response)))
      (println (str "response 1:" body))
      (let [response-2 ((app-handler abspath) (with-session
                                                (mock-castra "/app/tenant"
                                                             'core.services.tenant.manage/create-tenant
                                                             details)
                                                (response-session response)))
            body-2 (parse-resp response-2)]
        (println (str "session id 2: " (response-session response-2)))
        (println (str "response 2: " body-2))))))
