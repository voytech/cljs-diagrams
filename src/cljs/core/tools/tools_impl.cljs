(ns core.tools.tools-impl
  (:require [core.tools.tool :as tool]
            [utils.dom.dom-utils :refer [by-id]]
            [core.entities.entities-impl :as entities]
            [impl.api.shared.resources :as ra]
            [tailrecursion.hoplon :refer [img]]
            [core.project.project :as canvas]))

(defn photo-tool [name desc icon res-type]
  (tool/create-tool name desc "photo" icon (fn [tool ctx]
                                             (let [resource (ra/get-resource (:name tool) res-type)]
                                               (-> (entities/create-image-entity (img :src (ra/resource-server-path resource)) ctx)
                                                   (canvas/add-entity))))))

(defn background-tool [name desc icon]
  (tool/create-tool name desc "background" icon  (fn [tool ctx]
                                                   (let [resource (ra/get-resource (:name tool) :background)]
                                                     (-> (entities/create-background-entity (img :src (ra/resource-server-path resource)) ctx)
                                                         (canvas/set-background))))))

(defn slot-tool []
  (tool/create-tool "Placeholder"
                    "Insert other elements to fit placeholder"
                    "placeholder"
                    icon
                    (fn [tool ctx]
                      (-> (entities/create-slot-entity ctx)
                          (canvas/add-entity)))))

(defn text-tool []
  (tool/create-tool "Write Text!"
                    "Drag and write a text here."
                    "text"
                    icon
                    (fn [tool ctx]
                      (-> (entities/create-text-entity "Enter text..." ctx)
                          (canvas/add-entity)))))
