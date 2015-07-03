(ns core.tools.tools-impl
  (:require [core.tools.tool :as tool]
            [utils.dom.dom-utils :refer [by-id]]
            [core.entities.entities-impl :as entities]
            [core.project-resources :as res]
            [tailrecursion.hoplon :refer [img]]
            [core.canvas-interface :as canvas]))

(defn photo-tool [name desc icon res-type]
  (tool/create-tool name desc "photo" icon (fn [src ctx]
                                             (let [resource (res/find-resource (:name src) res-type)]
                                               (-> (entities/create-image-entity
                                                    (img :src (:content resource))
                                                    ctx)
                                                   (canvas/add-entity))))))

(defn background-tool [name desc icon]
  (tool/create-tool name desc "background" icon  (fn [src ctx]
                                                   (let [resource (res/find-resource (:name src) res/BACKGROUND)]
                                                     (-> (entities/create-background-entity
                                                          (img :src (:content resource))
                                                          ctx)
                                                         (canvas/set-background))))))

(defn slot-tool []
  (tool/create-tool "Placeholder"
                    "Insert other elements to fit placeholder"
                    "placeholder"
                    icon
                    (fn [src ctx]
                      (-> (entities/create-slot-entity ctx)
                          (canvas/add-entity)))))

(defn text-tool []
  (tool/create-tool "Write Text!"
                    "Drag and write a text here."
                    "text"
                    icon
                    (fn [src ctx]
                      (-> (entities/create-text-entity "Enter text..." ctx)
                          (canvas/add-entity)))))
