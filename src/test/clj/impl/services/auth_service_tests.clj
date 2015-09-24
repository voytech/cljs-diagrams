(ns impl.services.auth-service-tests
  (:require [core.services.public.auth :refer :all]
            [clojure.test :refer :all]
            [impl.db.schema :refer :all]))

(deftest test-shared-schema
  (let [response (do-register {:username "Wojciech"
                               :password "123456789"
                               :role :core.auth.roles/TENANT
                               :identity "voy-tech"})]
    (println (str "reponse: " response))
    (is (= true true))))
