(ns core.db.manager-tests
  (:require [clojure.test :refer :all]
            [core.db.manager :refer :all]))

;Wait !! for test purposes - only mem db.
(deftest test-load-properties
  (is (= "datomic:mem://testing" (:db-url (load-configuration "resources/schema/test_properties.edn")))))

(deftest test-load-schema
  (is (not (nil? (load-file "resources/schema/public_schema.edn")))))

(deftest test-initialize-db
  (let [schema-map (load-file "resources/schema/public_schema.edn")
        init-result @(initialize-database SHARED-DB schema-map)]
    (println init-result)
    (is (not (nil? init-result)))
    (is (not (nil? (-> init-result :db-after))))))
