(ns impl.services.tenant.templates-test
  (:require [core.services.tenant.templates-service :refer :all]
            [app :refer [app-handler]]
            [impl.services.test-helpers :refer :all]
            [clojure.test :refer :all]
            [tailrecursion.cljson  :as e :refer [cljson->clj clj->cljson]]
            [ring.mock.request :as mock]
            [impl.db.schema :refer :all]
            [core.db.schemap :refer [persist-schema]]
            [datomic.api :as d]
            [core.services.base :refer :all]))

(defn tenant-session-fixture [f]
  (let [payload {:username "Test001"
                 :password "UUUZDDD"
                 :re-password "UUUZDDD"
                 :role :core.auth.roles/TENANT
                 :identity "test001"}
        details {:firstname "Test 001"
                 :lastname "Test 001"
                 :email "adrian.monk@police.com"
                 :address-line-1 "San Francisco."}]

    (let [response (->>
                    (castra-request "/app/public" 'core.services.public.auth/register payload)
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.manage/create-tenant details))])
    (f)))

(use-fixtures :once tenant-session-fixture)

(deftest test-initialize-template
  (let [response (->>
                  (auth-request "/app/login" "Test001" "UUUZDDD")
                  (response-session)
                  (castra-request "/app/tenant" 'core.services.tenant.templates-service/create-template "A sample template!"))]))

(deftest test-create-new-template
  (let [template {:name "New template"
                  :info "This is a new template"
                  :page-count 8
                  :fixed-page-count false
                  :max-page-count 4
                  :format :project.template.format/A4
                  :custom-format? false
                  :notes "More description on this template goes here."}]
    (let [response (->>
                    (auth-request "/app/login" "Test001" "UUUZDDD")
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.templates-service/save-template template))])))

(deftest test-create-and-alter-template
  (let [template {:name "New template 2"
                  :info "This is a new template"
                  :page-count 8
                  :fixed-page-count false
                  :max-page-count 4
                  :format :project.template.format/A4
                  :custom-format? false
                  :notes "More description on this template goes here."}
        new-template {:name "New template 2"
                      :info "Info has changed"
                      :notes "Notes has changed"}]
    (let [response (->>
                    (auth-request "/app/login" "Test001" "UUUZDDD")
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.templates-service/save-template template)
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.templates-service/save-template new-template))])))


;;(deftest test-add-template-with-multi-formats)

(deftest test-query-all-templates
  (let [name "template"]
    (let [response (->>
                    (auth-request "/app/login" "Test001" "UUUZDDD")
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.templates-service/get-templates {}))])))

(deftest test-create-and-get-template
  (let [template {:name "New template 3"
                  :info "This is a new template"
                  :page-count 7
                  :fixed-page-count false
                  :max-page-count 4
                  :format :project.template.format/A4
                  :custom-format? false
                  :notes "More description on this template goes here."}]
    (let [response (->>
                    (auth-request "/app/login" "Test001" "UUUZDDD")
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.templates-service/save-template template)
                    (response-session)
                    (castra-request "/app/tenant" 'core.services.tenant.templates-service/get-template "New template 3"))])))
