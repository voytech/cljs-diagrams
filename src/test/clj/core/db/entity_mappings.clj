(ns core.db.entity-mappings
  (:require [clojure.test :refer :all]
            [core.db.entities :refer :all]))

;Wait !! for test purposes - only mem db.
(deftest test-defentity-macro
  (pre (defentity 'user-login
         (from :username to :user/name     with {:required true})
         (from :password to :user/password with {:required true})
         (from :roles    to :user/roles    with {:required true})
         (from :tenant   to :user/tenant   with {:lookup-ref #([:user/name %])}))))
