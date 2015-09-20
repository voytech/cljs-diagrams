(ns core.db.entity-mappings-tests
  (:require [clojure.test :refer :all]
            [core.db.entities :refer :all]
            [core.db.manager :refer :all]
            [datomic.api :as d]))

(defn- h2-db-url []
  (str (:db-url (load-configuration "resources/schema/properties.edn")) "/testing"))

(defn- mem-db-url []
  (:db-url (load-configuration "resources/schema/test_properties.edn")))

(defn- init-in-memory-db []
  (initialize-database (mem-db-url) (load-file "resources/schema/public_schema.edn")))

(defn- find-property-named [prop-name dburl]
  (let [db (d/db (d/connect dburl))]
    (:db/ident (first (first (d/q '[:find (pull ?p [:db/ident])
                                    :in $ ?name
                                    :where [?p :db/ident ?name]] db prop-name))))))

(defn deep-find
  [m k]
  (->> (tree-seq map? vals m)
       (filter map?)
       (some k)))

(deftest test-defentity-macro
  (defschema 'test-defentity-macro
             {:mapping-inference true
              :auto-persist-schema false
              :db-url (mem-db-url)}
    (defentity 'user.login
      (property name :username  type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password  type :db.type/string                            mapping-opts {:required true})
      (property name :roles     type :db.type/ref                               mapping-opts {:required true})
      (property name :tenant    type :db.type/ref                               mapping-opts {:lookup-ref #([:user.login/username %])}))
    (defentity 'tenant.login
      (property name :username      type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password      type :db.type/string                            mapping-opts {:required true})
      (property name :dburl         type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :organization  type :db.type/string unique :db.unique/identity mapping-opts {:required true})))
  (let  [user-login (get-var 'user.login)
         tenant-login (get-var 'tenant.login)]
    (is (= 'user.login (:type user-login)))
    (is (= :db.type/string (-> user-login :mapping :username :type)))
    (is (= :db.unique/identity (-> user-login :mapping :username :unique)))
    (is (= :user.login/username (-> user-login :mapping :username :to-property)))
    (is (= :db.type/string (-> user-login :mapping :password :type)))
    (is (= :user.login/password (-> user-login :mapping :password :to-property)))
    (is (= :db.type/ref (-> user-login :mapping :roles :type)))
    (is (= :user.login/roles (-> user-login :mapping :roles :to-property)))
    (is (= :db.type/ref (-> user-login :mapping :tenant :type)))
    (is (= :user.login/tenant (-> user-login :mapping :tenant :to-property))))
  (let [this-schema (-> schema :test-defentity-macro)]
    (println (keys schema))
    (println (-> this-schema :data))
    (println (-> this-schema :mapping-opts)))
  (is (not (nil? (get-frequencies)))))

(deftest test-defentity-macro-lazy-schema
  (defschema 'test-defentity-macro
             {:mapping-inference true
              :auto-persist-schema true
              :db-drop true
              :db-url (mem-db-url)}
    (defentity 'user.login
      (property name :username  type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password  type :db.type/string                            mapping-opts {:required true})
      (property name :roles     type :db.type/ref                               mapping-opts {:required true})
      (property name :tenant    type :db.type/ref                               mapping-opts {:lookup-ref #([:user.login/username %])}))
    (defentity 'tenant.login
      (property name :username      type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password      type :db.type/string                            mapping-opts {:required true})
      (property name :dburl         type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :organization  type :db.type/string unique :db.unique/identity mapping-opts {:required true})))
  (is (= :user.login/username (find-property-named :user.login/username (mem-db-url))))
  (is (= :user.login/password (find-property-named :user.login/password (mem-db-url))))
  (is (= :user.login/roles (find-property-named :user.login/roles (mem-db-url))))
  (is (= :user.login/tenant (find-property-named :user.login/tenant (mem-db-url))))
  (is (= :entity/type (find-property-named :entity/type (mem-db-url))))
  (is (not (nil? (get-frequencies)))))


(deftest test-resolve-mapping
  (defschema 'test-resolve-mapping
             {:mapping-inference true
              :auto-persist-schema false
              :db-url (mem-db-url)}
    (defentity 'user.login
      (property name :username  type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password  type :db.type/string                            mapping-opts {:required true})
      (property name :roles     type :db.type/ref                               mapping-opts {:required true})
      (property name :tenant    type :db.type/ref                               mapping-opts {:lookup-ref #([:user.login/username %])}))
    (defentity 'tenant.login
      (property name :username      type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password      type :db.type/string                            mapping-opts {:required true})
      (property name :dburl         type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :organization  type :db.type/string unique :db.unique/identity mapping-opts {:required true})))

  (let [entity {:username "Wojtek"
                :password "sdakdshd"
                :dburl    "localhost:432"}]
    (is (= 'tenant.login (-> (find-mapping entity)
                             (:type))))))

(deftest test-cannot-resolve-mapping
  (defschema 'test-cannot-resolve-mapping
             {:mapping-inference true
              :auto-persist-schema false
              :db-url (mem-db-url)}
    (defentity 'user.login
      (property name :username  type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password  type :db.type/string                            mapping-opts {:required true})
      (property name :roles     type :db.type/ref                               mapping-opts {:required true})
      (property name :tenant    type :db.type/ref                               mapping-opts {:lookup-ref #([:user.login/username %])}))
    (defentity 'tenant.login
      (property name :username      type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password      type :db.type/string                            mapping-opts {:required true})
      (property name :dburl         type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :organization  type :db.type/string unique :db.unique/identity mapping-opts {:required true})))
  (let [entity {:username "Wojtek"
                :password "asdjkhasd"}]
    (is (not (nil? (get-frequencies))))
    (println (get-frequencies))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Cannot determine mapping. At least two mappings with same frequency"
                          (find-mapping entity)))))

(deftest test-mapping-lookup-ref []
  (defschema 'test-mapping-lookup-ref
             {:mapping-inference true
              :auto-persist-schema false
              :db-url (mem-db-url)}
    (defentity 'user.login
      (property name :username  type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password  type :db.type/string                            mapping-opts {:required true})
      (property name :roles     type :db.type/ref                               mapping-opts {:required true})
      (property name :tenant    type :db.type/ref                               mapping-opts {:lookup-ref (fn [v] [:user.login/username v])}))
    (let [entity-vec {:username "Wojtek"
                      :password "adasd"
                      :tenant "Obx"}
          db (clj->db entity-vec)]
      (is (= "Wojtek" (:user.login/username db)))
      (is (= "adasd" (:user.login/password db)))
      (is (= [:user.login/username "Obx"] (:user.login/tenant db)))
      (is (= :user.login (:entity/type db))))))

(deftest test-mapping-forth-and-back-simple []
  (defschema 'test-mapping-forth-and-back-simple
             {:mapping-inference true
              :auto-persist-schema false
              :db-url (mem-db-url)}
       (defentity 'user.login
            (property name :username  type :db.type/string unique :db.unique/identity mapping-opts {:required true})
            (property name :password  type :db.type/string                            mapping-opts {:required true})
            (property name :roles     type :db.type/ref                               mapping-opts {:required true})
            (property name :tenant    type :db.type/ref                               mapping-opts {:lookup-ref (fn [v] [:user.login/username v])}))
       (defentity 'tenant.login
            (property name :username      type :db.type/string unique :db.unique/identity mapping-opts {:required true})
            (property name :password      type :db.type/string                            mapping-opts {:required true})
            (property name :dburl         type :db.type/string unique :db.unique/identity mapping-opts {:required true})
            (property name :users         type :db.type/ref                               mapping-opts {:ref-type 'user.login})
            (property name :organization  type :db.type/string unique :db.unique/identity mapping-opts {:required false})))
    (let [entity-vec {:username "Wojtek"
                      :password "adasd"
                      :dburl    "localhost:432"}
          db (clj->db entity-vec)
          clj (db->clj db)]
      (is (= "Wojtek" (:tenant.login/username db)))
      (is (= "adasd" (:tenant.login/password db)))
      (is (= "localhost:432" (:tenant.login/dburl db)))
      (is (= :tenant.login (:entity/type db)))
      (is (= "Wojtek" (:username clj)))
      (is (= "adasd" (:password clj)))
      (is (= "localhost:432" (:dburl clj)))))

(deftest test-mapping-forth-and-back-compound[]
  (defschema 'test-mapping-forth-and-back-compound
             {:mapping-inference true
              :auto-persist-schema false
              :db-url (mem-db-url)}
    (defentity 'user.login
      (property name :username  type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password  type :db.type/string                            mapping-opts {:required true})
      (property name :roles     type :db.type/ref                               mapping-opts {:required true})
      (property name :tenant    type :db.type/ref                               mapping-opts {:lookup-ref (fn [v] [:user.login/username v])}))
    (defentity 'tenant.login
      (property name :username      type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password      type :db.type/string                            mapping-opts {:required true})
      (property name :dburl         type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :users         type :db.type/ref                               mapping-opts {:ref-type 'user.login})
      (property name :organization  type :db.type/string unique :db.unique/identity mapping-opts {:required false})))
  (let [entity-vec {:username "Wojtek"
                    :password "adasd"
                    :dburl    "localhost:432"
                    :users [{:username "Jack"
                             :password "Jack1"
                             :roles "USER"}
                            {:username "tedd"
                             :password "tedd1"
                             :roles "USER"}]}
        db (clj->db entity-vec)
        clj (db->clj db)]
      (is (= "Wojtek" (:tenant.login/username db)))
      (is (= "adasd" (:tenant.login/password db)))
      (is (= "localhost:432" (:tenant.login/dburl db)))
      (is (= :tenant.login (:entity/type db)))
      (is (= "Jack" (get-in db [:tenant.login/users 0 :user.login/username])))
      (is (= "Jack1" (get-in db [:tenant.login/users 0 :user.login/password])))
      (is (= "USER" (get-in db [:tenant.login/users 0 :user.login/roles])))
      (is (= "tedd" (get-in db [:tenant.login/users 1 :user.login/username])))
      (is (= "tedd1" (get-in db [:tenant.login/users 1 :user.login/password])))
      (is (= "USER" (get-in db [:tenant.login/users 1 :user.login/roles])))
      (is (= "Wojtek" (:username clj)))
      (is (= "adasd" (:password clj)))
      (is (= "localhost:432" (:dburl clj)))
      (is (= "Jack" (get-in clj [:users 0 :username])))
      (is (= "Jack1" (get-in clj [:users 0 :password])))
      (is (= "USER" (get-in clj [:users 0 :roles])))
      (is (= "tedd" (get-in clj [:users 1 :username])))
      (is (= "tedd1" (get-in clj [:users 1 :password])))
      (is (= "USER" (get-in clj [:users 1 :roles])))))

(deftest test-mapping-non-entity-colls []
  (defschema 'test-mapping-non-entity-colls
             {:mapping-inference true
              :auto-persist-schema false
              :db-url (mem-db-url)}
    (defentity 'user.login
      (property name :username  type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password  type :db.type/string                            mapping-opts {:required true})
      (property name :roles     type :db.type/ref)
      (property name :tenant    type :db.type/ref                               mapping-opts {:lookup-ref (fn [v] [:tenant.login/username v])}))
    (defentity 'tenant.login
      (property name :username      type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password      type :db.type/string                            mapping-opts {:required true})
      (property name :dburl         type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :users         type :db.type/ref                               mapping-opts {:ref-type 'user.login})
      (property name :organization  type :db.type/string unique :db.unique/identity mapping-opts {:required false})))
  (let [entity-vec {:username "Wojtek"
                    :password "adasd"
                    :roles [:core.auth.roles/USER :core.auth.roles/TENANT]}
        db (clj->db entity-vec)
        clj (db->clj db)]
    (is (= "Wojtek" (:user.login/username db)))
    (is (= "adasd" (:user.login/password db)))
    (is (= [:core.auth.roles/USER :core.auth.roles/TENANT] (:user.login/roles db)))
    (is (= :user.login (:entity/type db)))
    (is (= "Wojtek" (:username clj)))
    (is (= "adasd" (:password clj)))
    (is (= [:core.auth.roles/USER :core.auth.roles/TENANT] (:roles clj)))
    (is (= nil (:entity/type clj)))))


(deftest test-mapping-with-mapping-hooks[]
  (defschema 'test-mapping-non-entity-colls
             {:mapping-inference true
              :auto-persist-schema false
              :db-url (mem-db-url)}
    (defentity 'user.login
      (property name :username  type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password  type :db.type/string                            mapping-opts {:required true})
      (property name :roles     type :db.type/ref)
      (property name :tenant    type :db.type/ref                               mapping-hook (fn [property-value] [:user.login/username property-value])))
    (defentity 'tenant.login
      (property name :username      type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password      type :db.type/string                            mapping-opts {:required true})
      (property name :dburl         type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :users         type :db.type/ref                               mapping-opts {:ref-type 'user.login})
      (property name :organization  type :db.type/string unique :db.unique/identity mapping-opts {:required false})))
  (let [entity-vec {:username "Wojtek"
                    :password "adasd"
                    :tenant "empik"
                    :roles [:core.auth.roles/USER :core.auth.roles/TENANT]}
        db (clj->db entity-vec)
        clj (db->clj db)]
    (is (= "Wojtek" (:user.login/username db)))
    (is (= "adasd" (:user.login/password db)))
    (is (= [:core.auth.roles/USER :core.auth.roles/TENANT] (:user.login/roles db)))
    (is (= :user.login (:entity/type db)))
    (is (= [:user.login/username "empik"] (:user.login/tenant db)))
    (is (= "Wojtek" (:username clj)))
    (is (= "adasd" (:password clj)))
    (is (= [:core.auth.roles/USER :core.auth.roles/TENANT] (:roles clj)))
    (is (= nil (:entity/type clj)))))
