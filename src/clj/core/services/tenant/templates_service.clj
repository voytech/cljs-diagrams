(ns core.services.tenant.templates-service
  (:require [tailrecursion.castra :refer [defrpc ex error *session*]]
            [core.services.base :refer :all]))

(defn- default-template [name]
  {:name name
   :info "This is a template"
   :page-count 4
   :fixed-page-count false
   :max-page-count 4
   :format :project.template.format/A4
   :custom-format? false
   :notes "More description on this template goes here."})

(defrpc create-template [name]
  (let [template (default-template name)]
    (binding [*database-url* (tenant-db-url)]
      (when-let [id (store-entity template)]
        (println (str "saved template" id))
        (assoc template :id id)))))

(defrpc save-template [template-data]
  (binding [*database-url* (tenant-db-url)]
    (when-let [id (store-entity template-data)]
      (assoc template-data :id id))))

(defrpc update-property [keyval]
  {:rpc/query [(load-entity [:project.template/name (:name keyval)] (tenant-db-url))]}
  (binding [*database-url* (tenant-db-url)]
    (when-let [id (store-entity {:name (:name keyval),
                                 (:key keyval) (:value keyval)})]
      {:id id})))

(defn get-property
  ([name property]
     ))

(defrpc get-template [name]
  (load-entity [:project.template/name name] (tenant-db-url)))

(defn all-template-names [])

(defn all-templates [{:keys [page-nr items-per-page] :as paging-info}]
  (binding [*database-url* (tenant-db-url)]
    (query-by-property :project.template/name)))

(defrpc get-templates [paging-info]
  (all-templates paging-info))
