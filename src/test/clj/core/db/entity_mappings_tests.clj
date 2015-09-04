(ns core.db.entity-mappings-tests
  (:require [clojure.test :refer :all]
            [core.db.entities :refer :all]))

(deftest test-defentity-macro
  (init {:mapping-detection true}
       (defentity 'user-login
            (from :username to :user/name     with {:required true})
            (from :password to :user/password with {:required true})
            (from :roles    to :user/roles    with {:required true})
            (from :tenant   to :user/tenant   with {:lookup-ref #([:user/name %])}))))
