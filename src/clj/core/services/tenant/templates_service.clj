(ns core.services.tenant.templates-service
  (:require [tailrecursion.castra :refer [defrpc ex error *session*]]
            [core.services.base :refer :all]))

(defn- default-template []
  {:name (str (java.util.UUID/randomUUID))
   :info "This is a template"
   :page-count 4
   :fixed-page-count false
   :max-page-count 4
   :format :project.template.format/A4
   :custom-format? false
   :notes "More description on this template goes here."})

(defrpc create-template []
  (let [template (default-template)]
    (when-let [id (store-entity template)]
      (assoc template :id id))))

(defrpc save-template [template-data]
  (when-let [id (store-entity template-data)]
    (assoc template :id id)))

(defn- update-property [keyval]
  {:rpc/query [(load-entity [:project.template/name (:name keyval)] (tenant-db-url))]}
  (when-let [id (store-entity {:name (:name keyval),
                               (:key kayval) (:value keyval)})]
    {:id id}))

(defn get-property
  ([name property]
     ))

(defn load-template [name]
  {:rpc/query [(load-entity [:project.template/name (:name name)] (tenant-db-url))]})

(defn all-template-names []
  )

(defn all-templates []
  (binding [*database-url* (tenant-db-url)]
    (query-by-property :project.template/name)))

(defrpc get-templates [paging-info]
  {:rpc/query [(all-templates)]})
