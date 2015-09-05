(ns core.db.entity-mappings-tests
  (:require [clojure.test :refer :all]
            [core.db.entities :refer :all]))

(deftest test-defentity-macro
  (init {:mapping-detection true}
       (defentity 'user-login
            (from :username to :user/name     with {:required true})
            (from :password to :user/password with {:required true})
            (from :roles    to :user/roles    with {:required true})
            (from :tenant   to :user/tenant   with {:lookup-ref #([:user/name %])}))
       (defentity 'tenant-login
            (from :username to :user/name     with {:required true})
            (from :password to :user/password with {:required true})
            (from :dburl    to :tenant/dburl  with {:required true})
            (from :organization to :tenant/org with {:required true})))
  (is (not (nil? (entities-frequencies))))
)

(deftest test-resolve-mapping
  (init {:mapping-detection true}
       (defentity 'user-login
            (from :username to :user/name     with {:required true})
            (from :password to :user/password with {:required true})
            (from :roles    to :user/roles    with {:required true})
            (from :tenant   to :user/tenant   with {:lookup-ref #([:user/name %])}))
       (defentity 'tenant-login
            (from :username to :user/name     with {:required true})
            (from :password to :user/password with {:required true})
            (from :dburl    to :tenant/dburl  with {:required true})
            (from :organization to :tenant/org with {:required true})))
  (let [entity {:username "Wojtek"
                :password "Gudzunt"
                :dburl    "localhost:432"}]
    (is (= 'mappings.runtime/tenant-login (-> (find-mapping entity)
                                              (:type))))))

(deftest test-cannot-resolve-mapping
  (init {:mapping-detection true}
       (defentity 'user-login
            (from :username to :user/name     with {:required true})
            (from :password to :user/password with {:required true})
            (from :roles    to :user/roles    with {:required true})
            (from :tenant   to :user/tenant   with {:lookup-ref #([:user/name %])}))
       (defentity 'tenant-login
            (from :username to :user/name     with {:required true})
            (from :password to :user/password with {:required true})
            (from :dburl    to :tenant/dburl  with {:required true})
            (from :organization to :tenant/org with {:required true})))
  (let [entity {:username "Wojtek"
                :password "Gudzunt"}]
     (is (thrown-with-msg? clojure.lang.ExceptionInfo
                           #"Cannot determine mapping. At least two mappings with same frequency"
                           (find-mapping entity)))
    ))

(deftest test-map-entity-via-mapping-def
  (init {:mapping-detection true}
       (defentity 'user-login
            (from :username to :user/name     with {:required true})
            (from :password to :user/password with {:required true})
            (from :roles    to :user/roles    with {:required true})
            (from :tenant   to :user/tenant   with {:lookup-ref #([:user/name %])}))
       (defentity 'tenant-login
            (from :username to :user/name     with {:required true})
            (from :password to :user/password with {:required true})
            (from :dburl    to :tenant/dburl  with {:required true})
            (from :organization to :tenant/org with {:required true})))
  (let [entity {:username "Wojtek"
                :password "Gudzunt"
                :dburl    "localhost:432"}]
    (println (map-entity entity))
    ))
