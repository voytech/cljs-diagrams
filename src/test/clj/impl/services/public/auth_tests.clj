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
(defn- reload-db [name url]
  (d/delete-database url)
  (let [f (future (Thread/sleep 70000)
                  (when-let [res (d/create-database url)]
                    (println (str "create-db " res))
                    (when-let [connection (d/connect url)]
                      (persist-schema name url))))]
    @f))

(deftest test-register-rpc
  (reload-db 'shared *shared-db*)
  (let [payload {:username "Wojciech"
                 :password "UUUZDDD"
                 :re-password "UUUZDDD"
                 :role :core.auth.roles/TENANT
                 :identity "voytech"}
        details {:username (-> payload :username)
                 :firstname (-> payload :username)
                 :lastname "Maka"
                 :email "wojmak@gmail.com"
                 :address-line-1 "Piaseski"
                 :address-line-2 "ul. Gen GrochaWiejskiego"
                 :address-line-3 "12/13"}]
    (is (= (parse-resp ((app-handler abspath) (mock-castra "/app/public"
                                                           'core.services.public.auth/register
                                                           payload)))
           {:status  200
            :body (dissoc payload :password :re-password)}))
    (is (= (parse-resp ((app-handler abspath) (mock-castra "/app/public"
                                                           'core.services.public.auth/create-tenant
                                                           details)))
           {:status 200
            :body (dissoc details :external-id)}))))

(deftest test-create-tenant-rpc)
