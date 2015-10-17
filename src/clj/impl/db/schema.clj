(ns impl.db.schema
  (:require [core.db.schemap :refer :all]
            [core.db.manager :refer :all]
            [core.db.schemap-hooks :refer :all]))

(def TESTCONF "resources/schema/test_properties.edn")
(def DEVCONF "resources/schema/properties.edn")

(def ^:dynamic *conf-file* DEVCONF)
(def ^:dynamic *shared-db* (str (:db-url (load-configuration *conf-file*)) "/SHARED"))

(defn db-url
  ([] (str (:db-url (load-configuration *conf-file*)) "/"))
  ([name] (str (db-url) name)))

(defschema 'shared
    {:mapping-inference true
     :auto-persist-schema true
     :db-url *shared-db*}

    (defenum :core.auth.roles/TENANT)

    (defenum :core.auth.roles/USER)

    (defentity 'user.login
      (property name :username    type :db.type/string unique :db.unique/identity mapping-opts {:required true})
      (property name :password    type :db.type/string                            mapping-opts {:required true})
      (property name :identity    type :db.type/string)
      (property name :external-id type :db.type/uuid)
      (property name :licence     type :db.type/ref)
      (property name :role        type :db.type/ref    mapping-hook (fn [v] [:db/ident v]) reverse-mapping-hook (pull-property-hook :db/ident))
      (property name :tenant      type :db.type/ref    mapping-hook (fn [v] [:user.login/username v])
                reverse-mapping-hook (pull-property-hook :user.login/username)))

    (defentity 'app.licence
      (property name :name type :db.type/string unique :db.unique/identity)
      (property name :monthly-fee  type :db.type/float)
      (property name :commitment   type :db.type/long)))

(defschema 'tenant
  {:mapping-inference true
   :auto-persist-schema false}

  (defentity 'user.details
    (property name :username    type :db.type/string unique :db.unique/identity)
    (property name :firstname   type :db.type/string)
    (property name :lastname    type :db.type/string)
    (property name :email       type :db.type/string)
    (property name :address-line-1 type :db.type/string)
    (property name :address-line-2 type :db.type/string)
    (property name :address-line-3 type :db.type/string)
    (property name :external-id type :db.type/uuid))

  (defentity 'resource.category
    (property name :name type :db.type/string unique :db.unique/identity)
    (property name :description type :db.type/string))

  (defentity 'resource.file
    (property name :filename type :db.type/string unique :db.unique/identity)
    (property name :content-type :db.type/string)
    (property name :id type :db.type/string unique :db.unique/identity)
    (property name :category type :db.type/ref mapping-hook (fn [v] [:resource.category/name v]) reverse-mapping-hook (pull-property-hook :resource.category/name))
    (property name :path type :db.type/string)
    (property name :owner type :db.type/ref mapping-hook (fn [v] [:user.details/external-id v]) reverse-mapping-hook (pull-property-hook :user.details/external-id))
    (property name :data type :db.type/bytes))

  (defentity 'organization.details
    (property name :name type :db.type/string)
    (property name :logo type :db.type/ref)))
