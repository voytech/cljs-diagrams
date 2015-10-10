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

;;Ugly retracting and recreating database for test purposes.
(deftest test-register-rpc
  (reload-db 'shared *shared-db*)
  (let [payload {:username "Adrien"
                 :password "UUUZDDD"
                 :re-password "UUUZDDD"
                 :role :core.auth.roles/TENANT
                 :identity "adrien"}]
    (let [response ((app-handler abspath) (mock-castra "/app/public"
                                                       'core.services.public.auth/register
                                                       payload))
          body (parse-resp response)]
      (println (str "session id 1:" (response-session response)))
      (println (str "response 1:" body)))))

(deftest test-login
  (let [request (mock-login "/app/login" "Wojciech" "UUUZDDD")
        response ((app-handler abspath) request)]
    (is (not (nil? (response-session response))))))
