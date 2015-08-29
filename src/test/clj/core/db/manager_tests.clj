(ns core.db.manager-tests
  (:require [clojure.test :refer :all]
            [core.db.manager :refer :all]))

(deftest test-load-properties
  (is (= "datomic:free://localhost:4334" (:db-url (load-configuration "resources/schema/properties.edn")))))

(deftest test-load-schema [])
(deftest test-initialize-db
  (is (= 4 4)))
