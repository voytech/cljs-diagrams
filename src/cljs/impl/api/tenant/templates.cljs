(ns impl.api.tenant.templates
  (:require [tailrecursion.castra  :as c :refer [mkremote]]
            [tailrecursion.javelin :as j :refer [cell]]
            [core.project.settings :refer [settings!]]
            [core.project.project-services :refer [serialize-project-data
                                                   deserialize-project-data
                                                   cleanup-project-data]]
            [utils.dom.dom-utils :as dom])
  (:require-macros
   [tailrecursion.javelin :refer [defc defc= cell=]]))

(defc templates [])
(defc state {})
(defc error nil)
(defc loading [])
(defc current-template {})

(def save-template! (mkremote 'core.services.tenant.templates-service/save-template current-template error loading ["/app/tenant"]))
(def create-template! (mkremote 'core.services.tenant.templates-service/create-template current-template error loading ["/app/tenant"]))
(def update-template-property! (mkremote 'core.services.tenant.templates-service/update-property current-template error loading ["/app/tenant"]))
(def get-template! (mkremote 'core.services.tenant.templates-service/get-template current-template error loading ["/app/tenant"]))
(def get-templates! (mkremote 'core.services.tenant.templates-service/get-templates templates error loading ["/app/tenant"]))

(defn- index-of [coll v]
  (let [i (count (take-while #(not= v %) coll))]
    (when (or (< i (count coll))
            (= v (last coll)))
      i)))

(defn- by-name [name]
  (first (filter #(= name (:name %)) @templates)))

(defn- template-index-of [name]
  (index-of @templates (by-name name)))

(defn change-page [{:keys [page-nr items-per-page] :as paging-info}]
  (get-templates! paging-info))

(defn- update-property [property value]
  (when (not (nil? @current-template))
    (swap! current-template assoc-in [property] value)
    (save-template! @current-template)))

(defn get-property
  ([property] (cell= (when (not (nil? current-template))
                   (property current-template))))
  ([name property]
     (let [ind (template-index-of name)]
          (get-in @templates [ind property]))))

(defn init-templates []
  (get-templates! {})
  (cell= (when (not (nil? current-template))
           (let [pcount (:page-count current-template)]
             (settings! pcount :pages :count))))
  (cell= (when (not (nil? current-template))
           (let [format (:current-format current-template)]
             (settings! format :page-format)))))
